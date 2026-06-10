package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordChangeRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordResetConfirmRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.PasswordService;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.*;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.IdentifierNormalizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

/**
 * Local credential lifecycle implementation for reset/change/hash operations.
 * <p>
 * Integrates OTP verification, local-provider checks, user lifecycle validation, and session
 * revocation controls to contain credential compromise scenarios.
 */
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final OtpService otpService;
    private final SessionService sessionService;
    private final MeterRegistry meterRegistry;
    private final AppUserRepository appUserRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventLogger auditEventLogger;
    private final UserStateValidator userStateValidator;
    private final AuthCommonServices authCommonServices;

    /**
     * Initiates the password reset process by validating the provided identifier, checking user lifecycle state, and creating a password reset OTP. This method ensures that only users with valid local credentials can request a password reset and that the appropriate audit events are logged for monitoring and security purposes.
     *
     * @param identifier     The email or phone number used to identify the user requesting a password reset.
     * @param identifierType The type of identifier provided (EMAIL or PHONE).
     * @throws BusinessException If no user is found with the provided identifier, or if the user's lifecycle state does not allow for password reset requests.
     */
    @Override
    public void requestPasswordResetOtp(String identifier, IdentifierType identifierType) {
        AppUser user = findLocalUserForPasswordReset(identifier, identifierType);
        userStateValidator.validate(user, UserLifecycleOperation.PASSWORD_RESET_REQUEST);
        createPasswordResetOtp(identifier, identifierType);
        recordPasswordResetRequestSuccess(user.getId(), identifierType);
    }

    /**
     * Resets the user's password after verifying the password reset OTP. This method ensures that the user has a valid local credential and updates their password, revoking all active sessions.
     *
     * @param request The password reset confirmation request containing the OTP and new password.
     * @throws BusinessException If the OTP is invalid or expired, or if the user's lifecycle state does not allow for password reset confirmation.
     */
    @Override
    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        verifyPasswordResetOtp(request);

        AppUser appUser = findUserByIdentifier(request.identifier(), request.identifierType());
        userStateValidator.validate(appUser, UserLifecycleOperation.PASSWORD_RESET_CONFIRM);
        ensureLocalPasswordResetAllowed(appUser);
        updatePassword(appUser, request.newPassword());
        sessionService.revokeAllSessionsByUserId(appUser.getId());

        recordPasswordResetSuccess(appUser.getId(), request.identifierType());
    }

    /**
     * Changes the user's password after validating the current password and ensuring the new password meets the required criteria. This method ensures that the user has a valid local credential and updates their password, revoking all active sessions.
     *
     * @param request The password change request containing the current and new passwords.
     * @throws BusinessException If the current password is wrong. If the new password does not satisfy the required criteria.
     */
    @Override
    @Transactional
    public void changePassword(@Valid PasswordChangeRequest request) {
        AppUser appUser = authCommonServices.findAuthenticatedUser();

        userStateValidator.validate(appUser, UserLifecycleOperation.PASSWORD_CHANGE);
        ensureLocalPasswordChangeAllowed(appUser);
        ensureOldPasswordMatches(request.oldPassword(), appUser);
        ensureNewPasswordIsDifferent(request.oldPassword(), request.newPassword());
        updatePassword(appUser, request.newPassword());
        appUserRepository.save(appUser);
        sessionService.revokeAllSessionsByUserId(appUser.getId());

        recordPasswordChangeSuccess(appUser.getId());
    }

    /**
     * Encodes a raw password using the configured password encoder.
     *
     * @param rawPassword The raw password to encode.
     * @return The encoded password.
     */
    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Ensures that the user is allowed to change their password.
     *
     * @param appUser The user for whom to check password change permissions.
     * @throws BusinessException If the user is not allowed to change their password.
     */
    private void ensureLocalPasswordChangeAllowed(AppUser appUser) {
        if (!userAuthProviderRepository.existsByUserIdAndProvider(appUser.getId(), LOCAL)) {
            throw new BusinessException(ErrorCode.PASSWORD_CHANGE_NOT_ALLOWED);
        }
    }

    /**
     * Ensures that the provided old password matches the user's current password.
     *
     * @param oldPassword The old password to verify.
     * @param appUser     The user for whom to check the password.
     * @throws BusinessException If the old password is incorrect.
     */
    private void ensureOldPasswordMatches(String oldPassword, AppUser appUser) {
        if (!passwordEncoder.matches(oldPassword, appUser.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD, "Current password is incorrect");
        }
    }

    /**
     * Ensures that the new password is different from the old password.
     *
     * @param oldPassword The old password.
     * @param newPassword The new password to verify.
     * @throws BusinessException If the new password is the same as the old password.
     */
    private void ensureNewPasswordIsDifferent(String oldPassword, String newPassword) {
        if (oldPassword != null && oldPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.SAME_PASSWORD, "New password must be different from current password");
        }
    }

    /**
     * Records a successful password change event.
     *
     * @param userId The ID of the user who changed their password.
     */
    private void recordPasswordChangeSuccess(UUID userId) {
        meterRegistry.counter("auth.password_change.success").increment();
        auditEventLogger.log("auth.password_change", userId, null, null, "success", null, null);
    }

    /**
     * Verifies the password reset OTP.
     *
     * @param request The password reset confirmation request.
     * @throws BusinessException If the OTP is invalid or expired.
     */
    private void verifyPasswordResetOtp(PasswordResetConfirmRequest request) {
        OtpVerificationStatus status = request.identifierType() == IdentifierType.EMAIL
                ? otpService.verifyEmailOtp(request.identifier(), request.otp(), OtpPurpose.PASSWORD_RESET)
                : otpService.verifyPhoneOtp(request.identifier(), request.otp(), OtpPurpose.PASSWORD_RESET);

        if (!status.isSuccess()) {
            recordPasswordResetFailure(status.getMessage());
            throw new BusinessException(ErrorCode.INVALID_INPUT, status.getMessage());
        }
    }

    /**
     * Finds a user by their identifier and identifier type.
     *
     * @param identifier     The identifier to search for.
     * @param identifierType The type of the identifier.
     * @return The found user.
     * @throws BusinessException If no user is found.
     */
    private AppUser findUserByIdentifier(String identifier, IdentifierType identifierType) {
        String normalizedIdentifier = IdentifierNormalizer.normalize(identifier, identifierType);
        return identifierType == IdentifierType.EMAIL
                ? appUserRepository.findByEmail(normalizedIdentifier)
                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                : appUserRepository.findByPhone(normalizedIdentifier)
                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Ensures that password reset is allowed for the specified user.
     *
     * @param appUser The user for whom to check password reset eligibility.
     * @throws BusinessException If password reset is not allowed for the user.
     */
    private void ensureLocalPasswordResetAllowed(AppUser appUser) {
        if (!userAuthProviderRepository.existsByUserIdAndProvider(appUser.getId(), LOCAL)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Password reset is available only for password-based accounts.");
        }
    }

    /**
     * Updates the password for the specified user.
     *
     * @param appUser     The user for whom to update the password.
     * @param newPassword The new password to set.
     */
    private void updatePassword(AppUser appUser, String newPassword) {
        appUser.setPassword(passwordEncoder.encode(newPassword));
    }

    /**
     * Records a successful password reset event.
     *
     * @param userId         The ID of the user who reset their password.
     * @param identifierType The type of the identifier used for the reset.
     */
    private void recordPasswordResetSuccess(UUID userId, IdentifierType identifierType) {
        meterRegistry.counter("auth.password_reset.success").increment();
        auditEventLogger.log("auth.password_reset.confirm", userId, null, null, "success", "identifier_type=" + identifierType, null);
    }

    /**
     * Records a failed password reset event.
     *
     * @param message The error message describing the failure.
     */
    private void recordPasswordResetFailure(String message) {
        meterRegistry.counter("auth.password_reset.failure").increment();
        auditEventLogger.log("auth.password_reset.confirm", null, null, null, "failure", message, null);
    }

    /**
     * Finds a local user for password reset based on their identifier and identifier type.
     *
     * @param identifier     The identifier to search for.
     * @param identifierType The type of the identifier.
     * @return The found user.
     * @throws BusinessException If no eligible user is found.
     */
    private AppUser findLocalUserForPasswordReset(String identifier, IdentifierType identifierType) {
        AppUser user = findUserByIdentifierOrNull(identifier, identifierType);
        if (user == null || !userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), LOCAL)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "No user found with the provided identifier for password reset");
        }
        return user;
    }

    /**
     * Creates a password-reset OTP for the specified identifier and identifier type.
     *
     * @param identifier     The identifier for which to create the OTP.
     * @param identifierType The type of the identifier.
     */
    private void createPasswordResetOtp(String identifier, IdentifierType identifierType) {
        String normalizedIdentifier = IdentifierNormalizer.normalize(identifier, identifierType);
        if (identifierType == IdentifierType.EMAIL) {
            otpService.createEmailOtp(normalizedIdentifier, OtpPurpose.PASSWORD_RESET);
            return;
        }
        otpService.createPhoneOtp(normalizedIdentifier, OtpPurpose.PASSWORD_RESET);
    }

    /**
     * Records a successful password reset request event.
     *
     * @param userId         The ID of the user who requested the password reset.
     * @param identifierType The type of the identifier used for the request.
     */
    private void recordPasswordResetRequestSuccess(UUID userId, IdentifierType identifierType) {
        meterRegistry.counter("auth.password_reset.requested").increment();
        auditEventLogger.log("auth.password_reset.request", userId, null, null, "success", "identifier_type=" + identifierType, null);
    }

    /**
     * Finds a user by their identifier and identifier type, returning null if not found.
     *
     * @param identifier     The identifier to search for.
     * @param identifierType The type of the identifier.
     * @return The found user or null if not found.
     */
    private AppUser findUserByIdentifierOrNull(String identifier, IdentifierType identifierType) {
        String normalizedIdentifier = IdentifierNormalizer.normalize(identifier, identifierType);
        return identifierType == IdentifierType.EMAIL
                ? appUserRepository.findByEmail(normalizedIdentifier).orElse(null)
                : appUserRepository.findByPhone(normalizedIdentifier).orElse(null);
    }
}


