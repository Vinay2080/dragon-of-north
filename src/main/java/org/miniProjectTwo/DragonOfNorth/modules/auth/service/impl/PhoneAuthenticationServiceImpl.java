package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthenticationService;
import org.miniProjectTwo.DragonOfNorth.modules.otp.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.IdentifierNormalizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.USER_NOT_FOUND;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.PHONE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

/**
 * Phone-based authentication service implementation.
 * <p>
 * Handles user registration, status checking, and verification completion for phone
 * identifiers. Manages password encryption, phone verification flags, and database
 * operations. Critical for phone authentication flow and user lifecycle management.
 *
 * @see AuthenticationServiceResolver for service selection
 * @see AuthCommonServices for shared authentication logic
 */
@Service
@RequiredArgsConstructor

public class PhoneAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
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

    private AppUserStatusFinderResponse buildStatusResponse(AppUser user) {
        return new AppUserStatusFinderResponse(
                true,
                userAuthProviderRepository.findAllByUserId(user.getId()).stream().map(UserAuthProvider::getProvider).distinct().toList(),
                user.isPhoneNumberVerified(),
                user.getAppUserStatus()
        );
    }

    private AppUser buildPhoneUser(AppUserSignUpRequest request) {
        AppUser appUser = new AppUser();
        appUser.setPhone(IdentifierNormalizer.normalizePhone(request.identifier()));
        appUser.setPassword(passwordEncoder.encode(request.password()));
        appUser.setAppUserStatus(ACTIVE);
        return appUser;
    }

    private void persistLocalProvider(AppUser savedUser) {
        UserAuthProvider localProvider = new UserAuthProvider();
        localProvider.setUser(savedUser);
        localProvider.setProvider(LOCAL);
        userAuthProviderRepository.save(localProvider);
    }

    private AppUser findUserByPhone(String identifier) {
        return appUserRepository.findByPhone(IdentifierNormalizer.normalizePhone(identifier))
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
    }

    private void prepareUserForSignup(AppUserSignUpRequest request, String normalizedIdentifier) {
        appUserRepository.findByPhoneForUpdate(normalizedIdentifier)
                .map(existingUser -> reactivateDeletedAccount(existingUser, request))
                .orElseGet(() -> createNewUser(request));
    }

    private AppUser createNewUser(AppUserSignUpRequest request) {
        AppUser savedUser = appUserRepository.save(buildPhoneUser(request));
        persistLocalProvider(savedUser);
        return savedUser;
    }

    private AppUser reactivateDeletedAccount(AppUser appUser, AppUserSignUpRequest request) {
        userStateValidator.validate(appUser, UserLifecycleOperation.LOCAL_SIGNUP_START);
        if (!userStateValidator.isDeleted(appUser)) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED,
                    UserLifecycleOperation.LOCAL_SIGNUP_START.name(),
                    appUser.getAppUserStatus().name());
        }

        appUser.setPassword(passwordEncoder.encode(request.password()));
        appUser.setPhoneNumberVerified(false);
        persistLocalProviderIfMissing(appUser);
        AppUser savedUser = appUserRepository.save(appUser);
        recordReactivationStarted(savedUser);
        return savedUser;
    }

    private void persistLocalProviderIfMissing(AppUser savedUser) {
        if (!userAuthProviderRepository.existsByUserIdAndProvider(savedUser.getId(), LOCAL)) {
            persistLocalProvider(savedUser);
        }
    }

    private void ensureSignupOtpVerified(String identifier) {
        OtpToken otpToken;
        try {
            otpToken = otpService.fetchLatest(IdentifierNormalizer.normalizePhone(identifier), PHONE, OtpPurpose.SIGNUP);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() != ErrorCode.OTP_NOT_FOUND) {
                throw ex;
            }
            throw new BusinessException(ErrorCode.OTP_VERIFICATION_REQUIRED);
        }

        if (otpToken == null || otpToken.isExpired() || otpToken.getVerifiedAt() == null) {
            throw new BusinessException(ErrorCode.OTP_VERIFICATION_REQUIRED);
        }
    }

    private void completePhoneSignup(AppUser appUser) {
        authCommonServices.assignDefaultRole(appUser);
        appUser.setPhoneNumberVerified(true);
        appUser.setAppUserStatus(ACTIVE);
        appUserRepository.save(appUser);
    }

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
