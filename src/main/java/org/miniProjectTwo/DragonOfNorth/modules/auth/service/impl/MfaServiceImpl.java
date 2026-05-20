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

    @Override
    @Transactional
    public MfaSetupConfirmResponse confirmMfaSetup(AuthRequestContext context, @NotNull @Length String code) {
        AppUser appUser = authCommonServices.findAuthenticatedUser();
        userStateValidator.validate(appUser, UserLifecycleOperation.MFA_SETUP_CONFIRM);

        if (appUser.isMfaEnabled()) {
            throw new BusinessException(ErrorCode.MFA_ALREADY_ENABLED, "MFA is already enabled for this account");
        }

        String encryptedSecret = redisTemplate.opsForValue().get(MFA_SETUP_KEY_PREFIX + appUser.getId());
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
        redisTemplate.delete(MFA_SETUP_KEY_PREFIX + appUser.getId());

        String[] recoveryCodes = recoveryCodeService.generateAndStoreRecoveryCodes(mfaSettings);
        recordMfaSetupConfirmSuccess(appUser.getId(), context);
        return new MfaSetupConfirmResponse(recoveryCodes);
    }

    @Override
    public boolean verifyMfaCode(AppUser appUser, @NotNull @Length String code) {
        return verifyTotpCode(appUser, code) || verifyRecoveryCode(appUser, code);
    }

    @Override
    public boolean verifyTotpCode(AppUser appUser, @NotNull @Length String code) {
        try {
            VerifyResult result = mfaVerificationService.verifyAtLogin(appUser, TOTP, code, MfaContext.empty());
            return result.success();
        } catch (BusinessException ex) {
            return false;
        }
    }

    @Override
    public boolean verifyRecoveryCode(AppUser appUser, @NotNull @Length String code) {
        try {
            VerifyResult result = mfaVerificationService.verifyAtLogin(appUser, RECOVERY_CODE, code, MfaContext.empty());
            return result.success();
        } catch (BusinessException ex) {
            return false;
        }
    }

    private void validateCode(String secret, @NotNull @Length String code) {
        if (!totpService.isValidCode(secret, code)) {
            throw new BusinessException(ErrorCode.MFA_INVALID_CODE, "Invalid MFA code");
        }
    }

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

    private void storeTemporaryMfaSecret(UUID id, String secret) {
        redisTemplate.opsForValue().set(MFA_SETUP_KEY_PREFIX + id, encryptionService.encrypt(secret), Duration.ofMinutes(5));
    }

    private void recordMfaRequestSuccess(UUID id, AuthRequestContext context) {
        meterRegistry.counter("auth.mfa_setup.request.success").increment();
        auditEventLogger.log("auth.mfa_setup.request", id, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }
}
