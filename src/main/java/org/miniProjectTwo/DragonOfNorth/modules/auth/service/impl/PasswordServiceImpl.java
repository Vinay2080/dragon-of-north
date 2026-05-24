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

    @Override
    public void requestPasswordResetOtp(String identifier, IdentifierType identifierType) {
        AppUser user = findLocalUserForPasswordReset(identifier, identifierType);
        userStateValidator.validate(user, UserLifecycleOperation.PASSWORD_RESET_REQUEST);
        createPasswordResetOtp(identifier, identifierType);
        recordPasswordResetRequestSuccess(user.getId(), identifierType);
    }

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

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    private void ensureLocalPasswordChangeAllowed(AppUser appUser) {
        if (!userAuthProviderRepository.existsByUserIdAndProvider(appUser.getId(), LOCAL)) {
            throw new BusinessException(ErrorCode.PASSWORD_CHANGE_NOT_ALLOWED);
        }
    }

    private void ensureOldPasswordMatches(String oldPassword, AppUser appUser) {
        if (!passwordEncoder.matches(oldPassword, appUser.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD, "Current password is incorrect");
        }
    }

    private void ensureNewPasswordIsDifferent(String oldPassword, String newPassword) {
        if (oldPassword != null && oldPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.SAME_PASSWORD, "New password must be different from current password");
        }
    }

    private void recordPasswordChangeSuccess(UUID userId) {
        meterRegistry.counter("auth.password_change.success").increment();
        auditEventLogger.log("auth.password_change", userId, null, null, "success", null, null);
    }

    private void verifyPasswordResetOtp(PasswordResetConfirmRequest request) {
        OtpVerificationStatus status = request.identifierType() == IdentifierType.EMAIL
                ? otpService.verifyEmailOtp(request.identifier(), request.otp(), OtpPurpose.PASSWORD_RESET)
                : otpService.verifyPhoneOtp(request.identifier(), request.otp(), OtpPurpose.PASSWORD_RESET);

        if (!status.isSuccess()) {
            recordPasswordResetFailure(status.getMessage());
            throw new BusinessException(ErrorCode.INVALID_INPUT, status.getMessage());
        }
    }

    private AppUser findUserByIdentifier(String identifier, IdentifierType identifierType) {
        String normalizedIdentifier = IdentifierNormalizer.normalize(identifier, identifierType);
        return identifierType == IdentifierType.EMAIL
                ? appUserRepository.findByEmail(normalizedIdentifier)
                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                : appUserRepository.findByPhone(normalizedIdentifier)
                  .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void ensureLocalPasswordResetAllowed(AppUser appUser) {
        if (!userAuthProviderRepository.existsByUserIdAndProvider(appUser.getId(), LOCAL)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Password reset is available only for password-based accounts.");
        }
    }

    private void updatePassword(AppUser appUser, String newPassword) {
        appUser.setPassword(passwordEncoder.encode(newPassword));
    }

    private void recordPasswordResetSuccess(UUID userId, IdentifierType identifierType) {
        meterRegistry.counter("auth.password_reset.success").increment();
        auditEventLogger.log("auth.password_reset.confirm", userId, null, null, "success", "identifier_type=" + identifierType, null);
    }

    private void recordPasswordResetFailure(String message) {
        meterRegistry.counter("auth.password_reset.failure").increment();
        auditEventLogger.log("auth.password_reset.confirm", null, null, null, "failure", message, null);
    }

    private AppUser findLocalUserForPasswordReset(String identifier, IdentifierType identifierType) {
        AppUser user = findUserByIdentifierOrNull(identifier, identifierType);
        if (user == null || !userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), LOCAL)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "No user found with the provided identifier for password reset");
        }
        return user;
    }

    private void createPasswordResetOtp(String identifier, IdentifierType identifierType) {
        String normalizedIdentifier = IdentifierNormalizer.normalize(identifier, identifierType);
        if (identifierType == IdentifierType.EMAIL) {
            otpService.createEmailOtp(normalizedIdentifier, OtpPurpose.PASSWORD_RESET);
            return;
        }
        otpService.createPhoneOtp(normalizedIdentifier, OtpPurpose.PASSWORD_RESET);
    }

    private void recordPasswordResetRequestSuccess(UUID userId, IdentifierType identifierType) {
        meterRegistry.counter("auth.password_reset.requested").increment();
        auditEventLogger.log("auth.password_reset.request", userId, null, null, "success", "identifier_type=" + identifierType, null);
    }

    private AppUser findUserByIdentifierOrNull(String identifier, IdentifierType identifierType) {
        String normalizedIdentifier = IdentifierNormalizer.normalize(identifier, identifierType);
        return identifierType == IdentifierType.EMAIL
                ? appUserRepository.findByEmail(normalizedIdentifier).orElse(null)
                : appUserRepository.findByPhone(normalizedIdentifier).orElse(null);
    }
}


