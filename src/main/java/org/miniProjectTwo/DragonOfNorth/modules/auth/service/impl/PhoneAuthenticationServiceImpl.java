package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthenticationService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.PasswordService;
import org.miniProjectTwo.DragonOfNorth.modules.otp.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.IdentifierNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.USER_NOT_FOUND;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.PHONE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

/**
 * Phone-identifier strategy for local sign-up/status/completion flows.
 * <p>
 * Mirrors email strategy while applying phone normalization and phone OTP verification semantics.
 * Keeps identifier-specific branching encapsulated behind {@link org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthenticationService}.
 */
@Service
@RequiredArgsConstructor

public class PhoneAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordService passwordService;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final AuthCommonServices authCommonServices;
    private final MeterRegistry meterRegistry;
    private final AuditEventLogger auditEventLogger;
    private final OtpService otpService;
    private final UserStateValidator userStateValidator;

    /**
     * Returns PHONE identifier type for service routing.
     * <p>
     * Used by AuthenticationServiceResolver to select this implementation
     * for phone-based authentication requests.
     *
     * @return PHONE identifier type
     */
    @Override
    public IdentifierType supports() {
        return PHONE;
    }

    /**
     * Retrieves user registration status for phone identifier.
     * <p>
     * Queries a database for phone-based user status or returns NOT_EXIST
     * if the user is not found. Critical for frontend authentication flow guidance.
     *
     * @param identifier phone number to check
     * @return user status response or NOT_EXIST status
     */
    @Override
    public AppUserStatusFinderResponse getUserStatus(String identifier) {
        meterRegistry.counter("auth.status_lookup.requested").increment();
        String normalizedIdentifier = IdentifierNormalizer.normalizePhone(identifier);
        return appUserRepository.findByPhone(normalizedIdentifier)
                .map(this::buildStatusResponse)
                .orElseGet(AppUserStatusFinderResponse::notFound);
    }

    /**
     * Creates a new user account with phone identifier.
     * <p>
     * Encrypts password, sets CREATED status, and persists user to a database.
     * Does not assign roles or verify phone - handled in completion flow.
     * Critical for user registration initiation.
     *
     * @param request sign-up request with phone and password
     * @return created user status response
     */
    @Override
    @Transactional
    public AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request) {
        String normalizedIdentifier = IdentifierNormalizer.normalizePhone(request.identifier());
        try {
            prepareUserForSignup(request, normalizedIdentifier);
            recordSignupSuccess();
            return getUserStatus(normalizedIdentifier);
        } catch (RuntimeException ex) {
            recordSignupFailure(ex);
            throw ex;
        }
    }

    /**
     * Completes user registration with phone verification.
     * <p>
     * Updates user status to VERIFIED, assigns a default USER role,
     * and persists changes. Does not set a phone verification flag.
     * Critical for enabling full authentication capabilities.
     *
     * @param identifier phone number to complete registration
     * @return updated user status response
     * @throws BusinessException if a user isn't found or already verified
     */
    @Override
    @Transactional
    public AppUserStatusFinderResponse completeSignUp(String identifier) {
        String normalizedIdentifier = IdentifierNormalizer.normalizePhone(identifier);
        try {
            AppUser appUser = findUserByPhone(normalizedIdentifier);
            userStateValidator.validate(appUser, UserLifecycleOperation.LOCAL_SIGNUP_COMPLETE);
            ensureSignupOtpVerified(normalizedIdentifier);
            completePhoneSignup(appUser);
            recordSignupCompleteSuccess(appUser);
            return getUserStatus(normalizedIdentifier);
        } catch (RuntimeException ex) {
            recordSignupCompleteFailure(ex);
            throw ex;
        }
    }

    /**
     * Builds a user status response for a given AppUser. Extracts authentication providers, phone verification status, and account status to construct a comprehensive response for frontend consumption. Critical for consistent API responses across different identifier types.
     *
     * @param user the AppUser entity for which to build the status response
     * @return the user status response
     */
    private AppUserStatusFinderResponse buildStatusResponse(AppUser user) {
        return new AppUserStatusFinderResponse(
                true,
                userAuthProviderRepository.findAllByUserId(user.getId()).stream().map(UserAuthProvider::getProvider).distinct().toList(),
                user.isPhoneNumberVerified(),
                user.getAppUserStatus()
        );
    }

    /**
     * Finds a user by their phone number. Normalizes the phone number and queries the database for a matching user. Throws a BusinessException with USER_NOT_FOUND error code if no user is found. Critical for ensuring that phone-based operations are performed on valid user accounts.
     *
     * @param identifier the phone number to search for
     * @return the found AppUser entity
     */
    private AppUser findUserByPhone(String identifier) {
        return appUserRepository.findByPhone(identifier)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
    }

    /**
     * Prepares a user account for phone-based sign-up. Checks if a user with the normalized phone number already exists. If a user exists and is deleted, it reactivates the account. If no user exists, it creates a new user account. Critical for handling both new registrations and reactivation in a seamless manner.
     *
     * @param request the sign-up request containing the phone number and password
     * @param normalizedIdentifier the normalized phone number to check for existing users
     */
    private void prepareUserForSignup(AppUserSignUpRequest request, String normalizedIdentifier) {
        appUserRepository.findByPhoneForUpdate(normalizedIdentifier)
                .map(existingUser -> reactivateDeletedAccount(existingUser, request))
                .orElseGet(() -> createNewUser(request));
    }

    /**
     * Reactivates a deleted user account for phone-based sign-up. Updates the user's password and verification status, and persists the local authentication provider. Critical for restoring access to previously deleted accounts.
     *
     * @param appUser the deleted AppUser entity to reactivate
     * @param request the sign-up request containing the new password
     * @return the reactivated AppUser entity
     */
    private AppUser reactivateDeletedAccount(AppUser appUser, AppUserSignUpRequest request) {
        userStateValidator.validate(appUser, UserLifecycleOperation.LOCAL_SIGNUP_START);
        if (!userStateValidator.isDeleted(appUser)) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED,
                    UserLifecycleOperation.LOCAL_SIGNUP_START.name(),
                    appUser.getAppUserStatus().name());
        }

        appUser.setPassword(passwordService.encodePassword(request.password()));
        appUser.setPhoneNumberVerified(false);
        persistLocalProviderIfMissing(appUser);
        AppUser savedUser = appUserRepository.save(appUser);
        recordReactivationStarted(savedUser);
        return savedUser;
    }

    /**
     * Creates a new user account for phone-based registration. Builds a new AppUser entity from the sign-up request, saves it to the database, and persists the local authentication provider. Critical for initiating the registration process for new users.
     *
     * @param request the sign-up request containing the phone number and password
     * @return the created AppUser entity
     */
    private AppUser createNewUser(AppUserSignUpRequest request) {
        AppUser savedUser = appUserRepository.save(buildPhoneUser(request));
        persistLocalProvider(savedUser);
        return savedUser;
    }

    /**
     * Persists a local authentication provider for a user if it is missing. Checks if a local provider already exists for the user, and if not, it creates and saves a new local provider. Critical for ensuring that reactivated accounts have the necessary authentication provider for local sign-in.
     *
     * @param savedUser the AppUser entity for which to check and persist the local provider
     */
    private void persistLocalProviderIfMissing(AppUser savedUser) {
        if (!userAuthProviderRepository.existsByUserIdAndProvider(savedUser.getId(), LOCAL)) {
            persistLocalProvider(savedUser);
        }
    }

    /**
     * Builds a new AppUser entity for phone-based registration. Normalizes the phone number, encodes the password, and sets the initial account status to ACTIVE. Critical for ensuring consistent user creation logic for phone identifiers.
     *
     * @param request the sign-up request containing the phone number and password
     * @return a new AppUser entity ready for persistence
     */
    private AppUser buildPhoneUser(AppUserSignUpRequest request) {
        String normalizedIdentifier = IdentifierNormalizer.normalizePhone(request.identifier());
        AppUser appUser = new AppUser();
        appUser.setPhone(normalizedIdentifier);
        appUser.setPassword(passwordService.encodePassword(request.password()));
        appUser.setAppUserStatus(ACTIVE);
        return appUser;
    }

    /**
     * Persists a local authentication provider for a given user. Associates the user with the LOCAL provider type and saves it to the database. Critical for enabling local authentication flows for the user.
     *
     * @param savedUser the AppUser entity for which to persist the local provider
     */
    private void persistLocalProvider(AppUser savedUser) {
        UserAuthProvider localProvider = new UserAuthProvider();
        localProvider.setUser(savedUser);
        localProvider.setProvider(LOCAL);
        userAuthProviderRepository.save(localProvider);
    }

    /**
     * Ensures that the OTP verification for phone-based sign-up is completed. Fetches the latest OTP token for the given phone number and SIGNUP purpose. If no OTP token is found, or if the token is expired or not verified, it throws a BusinessException with OTP_VERIFICATION_REQUIRED error code. Critical for enforcing phone verification before allowing users to complete the registration process.
     *
     * @param identifier the phone number for which to check OTP verification
     */
    private void ensureSignupOtpVerified(String identifier) {
        OtpToken otpToken;
        try {
            otpToken = otpService.fetchLatest(identifier, PHONE, OtpPurpose.SIGNUP);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() != ErrorCode.OTP_NOT_FOUND) {
                throw ex;
            }
            throw new BusinessException(ErrorCode.OTP_VERIFICATION_REQUIRED);
        }

        if (otpToken.isExpired() || otpToken.getVerifiedAt() == null) {
            throw new BusinessException(ErrorCode.OTP_VERIFICATION_REQUIRED);
        }
    }

    /**
     * Completes the phone-based sign-up process by assigning a default role, marking the phone number as verified, and updating the user's account status to ACTIVE. Saves the updated user entity to the database. Critical for finalizing the registration process and enabling full access for the user.
     *
     * @param appUser the AppUser entity to update and save
     */
    private void completePhoneSignup(AppUser appUser) {
        authCommonServices.assignDefaultRole(appUser);
        appUser.setPhoneNumberVerified(true);
        appUser.setAppUserStatus(ACTIVE);
        appUserRepository.save(appUser);
    }

    /**
     * Records a successful phone-based sign-up event. Increments the signup success counter and logs the event.
     */
    private void recordSignupSuccess() {
        meterRegistry.counter("auth.signup.success").increment();
        auditEventLogger.log("auth.signup", null, null, null, "success", "identifier_type=PHONE", null);
    }

    private void recordSignupFailure(RuntimeException ex) {
        meterRegistry.counter("auth.signup.failure").increment();
        auditEventLogger.log("auth.signup", null, null, null, "failure", ex.getMessage(), null);
    }

    private void recordSignupCompleteSuccess(AppUser appUser) {
        meterRegistry.counter("auth.signup.complete.success").increment();
        auditEventLogger.log("auth.signup.complete", appUser.getId(), null, null, "success", "identifier_type=PHONE", null);
    }

    private void recordSignupCompleteFailure(RuntimeException ex) {
        meterRegistry.counter("auth.signup.complete.failure").increment();
        auditEventLogger.log("auth.signup.complete", null, null, null, "failure", ex.getMessage(), null);
    }

    private void recordReactivationStarted(AppUser appUser) {
        meterRegistry.counter("auth.reactivation.started").increment();
        auditEventLogger.log("auth.reactivation", appUser.getId(), null, null, "started", "identifier_type=PHONE", null);
    }


}
