package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.MfaContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaSettings;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserMfaSettingsRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.TotpService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptionService;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link MfaProvider} for Time-based One-Time Password (TOTP) authentication.
 * <p>
 * This component provides the logic to verify TOTP codes during MFA challenges. It checks if TOTP is enabled for the user, retrieves the encrypted TOTP secret, decrypts it, and validates the provided code using the {@link TotpService}. Verification results are logged for auditing purposes.
 */
@Component
@RequiredArgsConstructor
public class TotpMfaProvider implements MfaProvider {
    private final UserMfaSettingsRepository userMfaSettingsRepository;
    private final EncryptionService encryptionService;
    private final TotpService totpService;
    private final AuditEventLogger auditEventLogger;

    /**
     * Returns the provider type for this MFA provider, which is TOTP.
     *
     * @return The provider type enum value representing TOTP.
     */
    @Override
    public ProviderType type() {
        return ProviderType.TOTP;
    }

    /**
     * Checks if TOTP MFA is enabled for the given user by verifying the presence of an encrypted TOTP secret in the user's MFA settings.
     *
     * @param user The user for whom to check TOTP MFA enablement.
     * @return True if TOTP MFA is enabled for the user, false otherwise.
     */
    @Override
    public boolean isEnabledFor(AppUser user) {
        if (user == null || !user.isMfaEnabled()) {
            return false;
        }

        return userMfaSettingsRepository.findByUserId(user.getId())
                .map(UserMfaSettings::getTotpSecretEncrypted)
                .map(secret -> !secret.isBlank())
                .orElse(false);
    }

    /**
     * Indicates that this MFA provider allows verification during the login challenge process.
     *
     * @return True, indicating that TOTP can be used for login challenges.
     */
    @Override
    public boolean allowsLoginChallenge() {
        return true;
    }

    /**
     * Indicates that this MFA provider allows verification for step-up authentication scenarios.
     *
     * @return True, indicating that TOTP can be used for step-up authentication.
     */
    @Override
    public boolean allowsStepUp() {
        return true;
    }

    /**
     * Verifies the provided TOTP code for the given user and MFA context. It checks if TOTP is enabled, retrieves and decrypts the user's TOTP secret, and validates the code using the TotpService. The verification result is logged for auditing purposes.
     *
     * @param user    The user for whom to verify the TOTP code.
     * @param code    The TOTP code provided by the user for verification.
     * @param context The MFA context containing additional information about the verification attempt.
     * @return A VerifyResult indicating whether the verification was successful or failed, along with any relevant failure reasons.
     */
    @Override
    public VerifyResult verify(AppUser user, String code, MfaContext context) {
        if (!isEnabledFor(user)) {
            VerifyResult result = VerifyResult.failure(type(), "TOTP_NOT_ENABLED");
            logVerification(user, context, result);
            return result;
        }

        String encryptedSecret = userMfaSettingsRepository.findByUserId(user.getId())
                .map(UserMfaSettings::getTotpSecretEncrypted)
                .orElse(null);

        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            VerifyResult result = VerifyResult.failure(type(), "TOTP_SECRET_MISSING");
            logVerification(user, context, result);
            return result;
        }

        String secret = encryptionService.decrypt(encryptedSecret);
        VerifyResult result = totpService.isValidCode(secret, code)
                ? VerifyResult.success(type())
                : VerifyResult.failure(type(), "INVALID_CODE");
        logVerification(user, context, result);
        return result;
    }
    private void logVerification(AppUser user, MfaContext context, VerifyResult result) {
        auditEventLogger.log("auth.mfa.provider.verify",
                user == null ? null : user.getId(),
                context == null ? null : context.deviceId(),
                context == null ? null : context.ipAddress(),
                result.success() ? "success" : "failure",
                "provider=" + type().getKey() + (result.failureReason() == null ? "" : ",reason=" + result.failureReason()),
                null);
    }
}
