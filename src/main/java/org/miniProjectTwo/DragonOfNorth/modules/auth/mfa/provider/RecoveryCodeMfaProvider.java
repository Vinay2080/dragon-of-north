package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.MfaContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserMfaSettingsRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.MfaRecoveryCodeService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.springframework.stereotype.Component;

/**
 * MFA provider implementation for recovery code verification. This provider allows users to authenticate using one-time recovery codes generated during MFA setup. It checks if recovery code MFA is enabled for the user, verifies the provided code against stored recovery codes, and logs verification attempts for auditing purposes. This provider is typically used as a fallback authentication method when primary MFA methods are unavailable.
 */
@Component
@RequiredArgsConstructor
public class RecoveryCodeMfaProvider implements MfaProvider {
    private final UserMfaSettingsRepository userMfaSettingsRepository;
    private final MfaRecoveryCodeService recoveryCodeService;
    private final AuditEventLogger auditEventLogger;

    /**
     * Returns the provider type for this MFA provider, which is RECOVERY_CODE.
     *
     * @return The provider type enum value representing recovery code MFA.
     */
    @Override
    public ProviderType type() {
        return ProviderType.RECOVERY_CODE;
    }

    /**
     * Checks if recovery code MFA is enabled for the given user. This method verifies that the user has MFA enabled and that recovery code settings are present in the database.
     *
     * @param user The user for whom to check if recovery code MFA is enabled.
     * @return true if recovery code MFA is enabled for the user, false otherwise.
     */
    @Override
    public boolean isEnabledFor(AppUser user) {
        if (user == null || !user.isMfaEnabled()) {
            return false;
        }

        return userMfaSettingsRepository.findByUserId(user.getId()).isPresent();
    }

    /**
     * Indicates that this MFA provider allows login challenge verification. Recovery code MFA can be used during the login process as a fallback authentication method.
     *
     * @return true, indicating that this provider supports login challenge verification.
     */
    @Override
    public boolean allowsLoginChallenge() {
        return true;
    }

    /**
     * Indicates that this MFA provider does not allow step-up verification. Recovery code MFA is typically used as a fallback method and is not intended for step-up authentication scenarios.
     *
     * @return false, indicating that this provider does not support step-up verification.
     */
    @Override
    public boolean allowsStepUp() {
        return false;
    }

    /**
     * Verifies the provided recovery code for the given user and authentication context. This method checks if recovery code MFA is enabled for the user, verifies the code against stored recovery codes, and logs the verification attempt for auditing purposes.
     *
     * @param user    The user attempting to verify with a recovery code.
     * @param code    The recovery code provided by the user for verification.
     * @param context The MFA authentication context, containing additional information about the authentication attempt.
     * @return A VerifyResult indicating whether the verification was successful or failed, along with any relevant failure reasons.
     */
    @Override
    public VerifyResult verify(AppUser user, String code, MfaContext context) {
        if (!isEnabledFor(user)) {
            VerifyResult result = VerifyResult.failure(type(), "RECOVERY_CODE_NOT_ENABLED");
            logVerification(user, context, result);
            return result;
        }

        boolean valid = userMfaSettingsRepository.findByUserId(user.getId())
                .map(settings -> recoveryCodeService.verifyAndConsumeRecoveryCode(settings, code))
                .orElse(false);

        VerifyResult result = valid ? VerifyResult.success(type()) : VerifyResult.failure(type(), "INVALID_CODE");
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
