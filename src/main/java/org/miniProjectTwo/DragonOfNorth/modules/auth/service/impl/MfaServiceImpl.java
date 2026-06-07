package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupConfirmResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.MfaContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.service.MfaVerificationService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaSettings;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserMfaSettingsRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.MfaRecoveryCodeService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.MfaService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.TotpService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptionService;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType.RECOVERY_CODE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType.TOTP;

@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {
    private static final String MFA_SETUP_KEY_PREFIX = "auth:mfa:setup";

    private final AuthCommonServices authCommonServices;
    private final UserStateValidator userStateValidator;
    private final MeterRegistry meterRegistry;
    private final AppUserRepository appUserRepository;
    private final AuditEventLogger auditEventLogger;
    private final StringRedisTemplate redisTemplate;
    private final EncryptionService encryptionService;
    private final UserMfaSettingsRepository userMfaSettingsRepository;
    private final TotpService totpService;
    private final MfaRecoveryCodeService recoveryCodeService;
    private final MfaVerificationService mfaVerificationService;

    /**
     * Initiates the MFA setup process for the authenticated user. This method generates a new TOTP secret, stores it temporarily in Redis with encryption, and returns the secret along with a QR code for the user to scan with their authenticator app. It also validates the user's state to ensure they are eligible for MFA setup and logs the request for auditing purposes. If MFA is already enabled for the user, a BusinessException is thrown.
     *
     * @param context The authentication request context containing information about the request, such as device ID and IP address.
     * @return An MfaSetupResponse containing the generated TOTP secret and QR code for MFA setup.
     * @throws BusinessException If MFA is already enabled for the user's account or if the user's state does not allow MFA setup.
     */
    @Override
    public MfaSetupResponse requestMfaSetup(AuthRequestContext context) {
        AppUser appUser = authCommonServices.findAuthenticatedUser();
        userStateValidator.validate(appUser, UserLifecycleOperation.MFA_SETUP_REQUEST);

        if (appUser.isMfaEnabled()) {
            throw new BusinessException(ErrorCode.MFA_ALREADY_ENABLED, "MFA is already enabled for this account");
        }

        String secret = totpService.generateSecret();
        storeTemporaryMfaSecret(appUser.getId(), secret);

        String qrCode = totpService.generateQrCode(secret, appUser);
        recordMfaRequestSuccess(appUser.getId(), context);
        return new MfaSetupResponse(secret, qrCode);

    }

    /**
     * Confirms the MFA setup for the authenticated user. This method validates the provided TOTP code against the temporary secret stored in Redis, persists the MFA settings, and enables MFA for the user. It also generates and stores recovery codes for the user.
     *
     * @param context The authentication request context containing information about the request, such as device ID and IP address.
     * @param code    The TOTP code to validate.
     * @return An MfaSetupConfirmResponse containing the generated recovery codes.
     * @throws BusinessException If the MFA setup session has expired or if the provided code is invalid.
     */
    @Override
    @Transactional
    public MfaSetupConfirmResponse confirmMfaSetup(AuthRequestContext context, @NotNull @Length String code) {
        AppUser appUser = authCommonServices.findAuthenticatedUser();
        userStateValidator.validate(appUser, UserLifecycleOperation.MFA_SETUP_CONFIRM);

        if (appUser.isMfaEnabled()) {
            throw new BusinessException(ErrorCode.MFA_ALREADY_ENABLED, "MFA is already enabled for this account");
        }

        String encryptedSecret = redisTemplate.opsForValue().getAndDelete(MFA_SETUP_KEY_PREFIX + appUser.getId());
        if (encryptedSecret == null) {
            throw new BusinessException(ErrorCode.MFA_SETUP_EXPIRED, "MFA setup session has expired. Please request again.");
        }

        String secret = encryptionService.decrypt(encryptedSecret);
        validateCode(secret, code);

        Instant enabledAt = Instant.now();
        UserMfaSettings mfaSettings = persistMfaSettings(appUser, encryptedSecret, enabledAt);
        appUser.setMfaEnabled(true);
        appUser.setMfaEnabledAt(enabledAt);

        appUserRepository.save(appUser);
        String[] recoveryCodes = recoveryCodeService.generateAndStoreRecoveryCodes(mfaSettings);
        recordMfaSetupConfirmSuccess(appUser.getId(), context);
        return new MfaSetupConfirmResponse(recoveryCodes);
    }

    /**
     * Verifies an MFA code against the user's configured MFA settings.
     *
     * @param appUser The user for whom to verify the code.
     * @param code    The MFA code to verify.
     * @return true if the code is valid, false otherwise.
     */
    @Override
    public boolean verifyMfaCode(AppUser appUser, @NotNull @Length String code) {
        return verifyTotpCode(appUser, code) || verifyRecoveryCode(appUser, code);
    }

    /**
     * Verifies a TOTP code against the user's configured MFA settings.
     *
     * @param appUser The user for whom to verify the code.
     * @param code The TOTP code to verify.
     * @return true if the code is valid, false otherwise.
     */
    @Override
    public boolean verifyTotpCode(AppUser appUser, @NotNull @Length String code) {
        try {
            VerifyResult result = mfaVerificationService.verifyAtLogin(appUser, TOTP, code, MfaContext.empty());
            return result.success();
        } catch (BusinessException ex) {
            return false;
        }
    }

    /**
     * Verifies a recovery code against the user's configured MFA settings.
     *
     * @param appUser The user for whom to verify the code.
     * @param code The recovery code to verify.
     * @return true if the code is valid, false otherwise.
     */
    @Override
    public boolean verifyRecoveryCode(AppUser appUser, @NotNull @Length String code) {
        try {
            VerifyResult result = mfaVerificationService.verifyAtLogin(appUser, RECOVERY_CODE, code, MfaContext.empty());
            return result.success();
        } catch (BusinessException ex) {
            return false;
        }
    }

    /**
     * Validates the provided TOTP code against the temporary secret.
     *
     * @param secret The temporary secret.
     * @param code The TOTP code to validate.
     */
    private void validateCode(String secret, @NotNull @Length String code) {
        if (!totpService.isValidCode(secret, code)) {
            throw new BusinessException(ErrorCode.MFA_INVALID_CODE, "Invalid MFA code");
        }
    }

    /**
     * Persists the MFA settings for the given user.
     *
     * @param appUser The user for whom to persist MFA settings.
     * @param encryptedSecret The encrypted TOTP secret.
     * @param enabledAt The timestamp when MFA was enabled.
     * @return The persisted MFA settings.
     */
    private UserMfaSettings persistMfaSettings(AppUser appUser, String encryptedSecret, Instant enabledAt) {
        UserMfaSettings settings = userMfaSettingsRepository.findByUserId(appUser.getId())
                .orElseGet(UserMfaSettings::new);
        settings.setUser(appUser);
        settings.setTotpSecretEncrypted(encryptedSecret);
        settings.setTotpEnabledAt(enabledAt);
        return userMfaSettingsRepository.save(settings);
    }


    private void recordMfaSetupConfirmSuccess(UUID id, AuthRequestContext context) {
        meterRegistry.counter("auth.mfa_setup.confirm.success").increment();
        auditEventLogger.log("auth.mfa_setup.confirm", id, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    /**
     * Stores the temporary MFA secret in Redis.
     *
     * @param id The user ID.
     * @param secret The temporary MFA secret.
     */
    private void storeTemporaryMfaSecret(UUID id, String secret) {
        redisTemplate.opsForValue().set(MFA_SETUP_KEY_PREFIX + id, encryptionService.encrypt(secret), Duration.ofMinutes(5));
    }

    private void recordMfaRequestSuccess(UUID id, AuthRequestContext context) {
        meterRegistry.counter("auth.mfa_setup.request.success").increment();
        auditEventLogger.log("auth.mfa_setup.request", id, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }
}
