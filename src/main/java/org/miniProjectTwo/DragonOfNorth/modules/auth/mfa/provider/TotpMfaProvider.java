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

@Component
@RequiredArgsConstructor
public class TotpMfaProvider implements MfaProvider {
    private final UserMfaSettingsRepository userMfaSettingsRepository;
    private final EncryptionService encryptionService;
    private final TotpService totpService;
    private final AuditEventLogger auditEventLogger;

    @Override
    public ProviderType type() {
        return ProviderType.TOTP;
    }

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

    @Override
    public boolean allowsLoginChallenge() {
        return true;
    }

    @Override
    public boolean allowsStepUp() {
        return true;
    }

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
