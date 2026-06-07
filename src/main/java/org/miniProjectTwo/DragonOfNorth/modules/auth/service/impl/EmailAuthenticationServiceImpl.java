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
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
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

import java.util.List;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.PENDING_VERIFICATION;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.USER_NOT_FOUND;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.EMAIL;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

/**
 * Email-identifier strategy for local sign-up/status/completion flows.
 * <p>
 * Responsibilities: normalize email identifiers, enforce lifecycle rules, coordinate OTP-gated
 * completion, provision LOCAL provider linkage, and return state snapshots used by auth UI routing.
 */
@Service
@RequiredArgsConstructor
public class EmailAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordService passwordService;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final AuthCommonServices authCommonServices;
    private final MeterRegistry meterRegistry;
    private final AuditEventLogger auditEventLogger;
    private final ProfileService profileService;
    private final OtpService otpService;
    private final UserStateValidator userStateValidator;


    /**
     * Returns EMAIL identifier type for service routing.
     * Used by AuthenticationServiceResolver to select this implementation
     * for email-based authentication requests.
     *
     * @return EMAIL identifier type
     */
    @Override
    public IdentifierType supports() {
        return EMAIL;
    }

    /**
     * Retrieves user registration status for email identifier.
     * <p>
     * Queries the database for an email-based user's current status.
     * <p>
     * If the user is not present in the database, this method returns a
     * {@code AppUserStatusFinderResponse.notFound()} result. For existing
     * users the response contains the current {@code AppUser.appUserStatus}
     * (for example {@code ACTIVE} or {@code LOCKED/DELETED}) along with
     * provider information and the email-verified flag. The frontend
     * consumes this information to decide the next authentication step.
     *
     * @param identifier email address to check
     * @return user status response or NOT_EXIST status
     */
    @Override
    public AppUserStatusFinderResponse getUserStatus(String identifier) {
        meterRegistry.counter("auth.status_lookup.requested").increment();
        String normalizedIdentifier = IdentifierNormalizer.normalizeEmail(identifier);
        return appUserRepository.findByEmail(normalizedIdentifier)
                .map(this::buildStatusResponse)
                .orElseGet(AppUserStatusFinderResponse::notFound);
    }

    /**
     * Creates a new user account with email identifier.
     * <p>
     * Encrypts the provided password, persists a new user record and sets the
     * initial account state. In the current model the service sets the user
     * {@code AppUser.appUserStatus} to {@code ACTIVE} and leaves
     * {@code emailVerified} as {@code false}. Role assignment and any
     * additional activation steps are handled in the completion flow.
     * This method is critical for initiating the user registration process.
     *
     * @param request sign-up request with email and password
     * @return created user status response
     */
    @Override
    @Transactional
    public AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request) {
        String normalizedIdentifier = IdentifierNormalizer.normalizeEmail(request.identifier());
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
     * Completes user registration with email verification.
     * <p>
     * Finalizes registration after verification (for example, after
     * successful OTP/email verification). This method assigns a default
     * USER role, sets the {@code emailVerified} flag to {@code true} and
     * persists changes transactionally so the user can fully authenticate.
     * Note: the method does not assume the use of intermediary statuses like
     * {@code CREATED} or {@code VERIFIED} — it updates verification state and
     * performs role provisioning according to the current status model
     * (e.g., the account may already be {@code ACTIVE}).
     *
     * @param identifier email address to complete registration
     * @return updated user status response
     * @throws BusinessException if a user isn't found or already verified
     */
    @Transactional
    @Override
    public AppUserStatusFinderResponse completeSignUp(String identifier) {
        String normalizedIdentifier = IdentifierNormalizer.normalizeEmail(identifier);
        try {
            AppUser appUser = findUserByEmail(normalizedIdentifier);
            userStateValidator.validate(appUser, UserLifecycleOperation.LOCAL_SIGNUP_COMPLETE);
            ensureSignupOtpVerified(normalizedIdentifier);
            completeEmailSignup(appUser);
            recordSignupCompleteSuccess(appUser);
            return getUserStatus(normalizedIdentifier);
        } catch (RuntimeException ex) {
            recordSignupCompleteFailure(ex);
            throw ex;
        }
    }

    /**
     * Builds a user status response for the given user.
     * <p>
     * This method constructs an {@code AppUserStatusFinderResponse} based on the
     * provided {@code AppUser} entity. It includes the user's current status,
     * email verification state, and a list of associated authentication providers.
     * The provider information is retrieved from the database and filtered to
     * include only distinct provider types linked to the user. This response is
     * used by the frontend to determine the appropriate next steps in the
     * authentication flow (e.g., whether to prompt for password, show email
     * verification, etc.).
     *
     * @param user the user entity for which to build the status response
     * @return an {@code AppUserStatusFinderResponse} containing the user's status and provider information
     */
    private AppUserStatusFinderResponse buildStatusResponse(AppUser user) {
        return new AppUserStatusFinderResponse(
                true,
                userAuthProviderRepository.findAllByUserId(user.getId())
                        .stream()
                        .map(UserAuthProvider::getProvider)
                        .distinct()
                        .toList(),
                user.isEmailVerified(),
                user.getAppUserStatus()
        );
    }

    /**
     * Finds a user by their email address.
     *
     * @param identifier the email address to search for
     * @return the user entity if found
     * @throws BusinessException if no user is found with the specified email
     */
    private AppUser findUserByEmail(String identifier) {
        return appUserRepository.findByEmail(identifier)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
    }

    /**
     * Prepares a user for sign-up by either reactivating a deleted account or creating a new one.
     *
     * @param request              the sign-up request
     * @param normalizedIdentifier the normalized email identifier
     */
    private void prepareUserForSignup(AppUserSignUpRequest request, String normalizedIdentifier) {
        appUserRepository.findByEmailForUpdate(normalizedIdentifier)
                .map(existingUser -> reactivateDeletedAccount(existingUser, request))
                .orElseGet(() -> createNewUser(request));
    }

    /**
     * Reactivates a deleted account for sign-up.
     *
     * @param appUser the user to reactivate
     * @param request the sign-up request
     * @return the reactivated user entity
     */
    private AppUser reactivateDeletedAccount(AppUser appUser, AppUserSignUpRequest request) {
        userStateValidator.validate(appUser, UserLifecycleOperation.LOCAL_SIGNUP_START);
        if (!userStateValidator.isDeleted(appUser)) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED,
                    UserLifecycleOperation.LOCAL_SIGNUP_START.name(),
                    appUser.getAppUserStatus().name());
        }

        appUser.setPassword(passwordService.encodePassword(request.password()));
        appUser.setEmailVerified(false);
        appUser.setAppUserStatus(PENDING_VERIFICATION);
        persistLocalProviderIfMissing(appUser);
        AppUser savedUser = appUserRepository.save(appUser);
        recordReactivationStarted(savedUser);
        return savedUser;
    }

    /**
     * Creates a new user for sign-up.
     *
     * @param request the sign-up request
     * @return the newly created user entity
     */
    private AppUser createNewUser(AppUserSignUpRequest request) {
        AppUser savedUser = appUserRepository.save(buildEmailUser(request));
        persistLocalProvider(savedUser);
        return savedUser;
    }

    /**
     * Persists the local authentication provider for a user if it doesn't already exist.
     *
     * @param savedUser the user for whom to persist the local provider
     */
    private void persistLocalProviderIfMissing(AppUser savedUser) {
        if (!userAuthProviderRepository.existsByUserIdAndProvider(savedUser.getId(), LOCAL)) {
            persistLocalProvider(savedUser);
        }
    }

    /**
     * Records the start of a reactivation process.
     *
     * @param appUser the user for whom to record the reactivation
     */
    private void recordReactivationStarted(AppUser appUser) {
        meterRegistry.counter("auth.reactivation.started").increment();
        auditEventLogger.log("auth.reactivation", appUser.getId(), null, null, "started", "identifier_type=EMAIL", null);
    }

    /**
     * Builds a new AppUser entity for email-based sign-up.
     * <p>
     * This method creates a new {@code AppUser} instance based on the provided
     * sign-up request. It normalizes the email identifier, encodes the password,
     * and sets the initial account status to {@code PENDING_VERIFICATION} with
     * the {@code emailVerified} flag set to {@code false}. This user entity is
     * then persisted to the database as part of the sign-up process. The method
     * encapsulates the logic for constructing a user entity that is ready for
     * email-based registration and subsequent verification steps.
     *
     * @param request the sign-up request containing the email and password
     * @return a new {@code AppUser} entity initialized for email-based sign-up
     */
    private AppUser buildEmailUser(AppUserSignUpRequest request) {
        String normalizedIdentifier = IdentifierNormalizer.normalizeEmail(request.identifier());
        AppUser user = new AppUser();
        user.setEmail(normalizedIdentifier);
        user.setPassword(passwordService.encodePassword(request.password()));
        user.setAppUserStatus(PENDING_VERIFICATION);
        user.setEmailVerified(false);
        return user;
    }

    /**
     * Persists a local authentication provider for the given user.
     *
     * @param savedUser the user for whom to persist the local provider
     */
    private void persistLocalProvider(AppUser savedUser) {
        UserAuthProvider localProvider = new UserAuthProvider();
        localProvider.setUser(savedUser);
        localProvider.setProvider(LOCAL);
        userAuthProviderRepository.save(localProvider);
    }

    /**
     * Ensures that the user has verified their email address via OTP before completing sign-up.
     *
     * @param identifier the email identifier
     * @throws BusinessException if the email is not verified
     */
    private void ensureSignupOtpVerified(String identifier) {
        OtpToken latestVerified = null;

        for (OtpPurpose purpose : List.of(OtpPurpose.SIGNUP, OtpPurpose.LOGIN_UNVERIFIED)) {
            try {
                OtpToken candidate = otpService.fetchLatest(identifier, EMAIL, purpose);
                if (candidate != null && !candidate.isExpired() && candidate.getVerifiedAt() != null) {
                    if (latestVerified == null || candidate.getVerifiedAt().isAfter(latestVerified.getVerifiedAt())) {
                        latestVerified = candidate;
                    }
                }
            } catch (BusinessException ex) {
                if (ex.getErrorCode() != ErrorCode.OTP_NOT_FOUND) {
                    throw ex;
                }
                // No OTP found for this purpose, continue checking other purposes
            }
        }

        if (latestVerified == null) {
            throw new BusinessException(ErrorCode.OTP_VERIFICATION_REQUIRED);
        }
    }

    /**
     * Completes the email sign-up process for a user.
     *
     * @param appUser the user for whom to complete the sign-up
     */
    private void completeEmailSignup(AppUser appUser) {
        authCommonServices.assignDefaultRole(appUser);
        appUser.setEmailVerified(true);
        appUser.setAppUserStatus(ACTIVE);
        appUserRepository.save(appUser);
        profileService.ensureProfileExists(appUser.getId(), null);
    }

    /**
     * Records a successful sign-up event.
     */
    private void recordSignupSuccess() {
        meterRegistry.counter("auth.signup.success").increment();
        auditEventLogger.log("auth.signup", null, null, null, "success", "identifier_type=EMAIL", null);
    }

    /**
     * Records a failed sign-up event.
     *
     * @param ex the exception that occurred during sign-up
     */
    private void recordSignupFailure(RuntimeException ex) {
        meterRegistry.counter("auth.signup.failure").increment();
        auditEventLogger.log("auth.signup", null, null, null, "failure", ex.getMessage(), null);
    }

    /**
     * Records a successful sign-up completion event.
     *
     * @param appUser the user for whom to record the completion
     */
    private void recordSignupCompleteSuccess(AppUser appUser) {
        meterRegistry.counter("auth.signup.complete.success").increment();
        auditEventLogger.log("auth.signup.complete", appUser.getId(), null, null, "success", "identifier_type=EMAIL", null);
    }

    /**
     * Records a failed sign-up completion event.
     *
     * @param ex the exception that occurred during sign-up completion
     */
    private void recordSignupCompleteFailure(RuntimeException ex) {
        meterRegistry.counter("auth.signup.complete.failure").increment();
        auditEventLogger.log("auth.signup.complete", null, null, null, "failure", ex.getMessage(), null);
    }


}
