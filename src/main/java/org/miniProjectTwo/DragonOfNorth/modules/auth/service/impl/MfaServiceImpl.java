package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Length;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupConfirmResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.MfaServices;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static dev.samstevens.totp.code.HashingAlgorithm.SHA1;

@Service
@RequiredArgsConstructor
@Slf4j

public class MfaServiceImpl implements MfaServices {
    private static final String MFA_SETUP_KEY_PREFIX = "auth:mfa:setup";

    private final AuthCommonServiceImpl authCommonServicesImpl;
    private final UserStateValidator userStateValidator;
    private final MeterRegistry meterRegistry;
    private final AppUserRepository appUserRepository;
    private final AuditEventLogger auditEventLogger;
    private final StringRedisTemplate redisTemplate;

    @Override
    public MfaSetupResponse requestMfaSetup(AuthRequestContext context) {
        AppUser appUser = authCommonServicesImpl.findAuthenticatedUser();
        userStateValidator.validate(appUser, UserLifecycleOperation.MFA_SETUP_REQUEST);

        if (appUser.isMfaEnabled()) {
            throw new BusinessException(ErrorCode.MFA_ALREADY_ENABLED, "MFA is already enabled for this account");
        }

        String secret = new DefaultSecretGenerator().generate();

        storeTemporaryMfaSecret(appUser.getId(), secret);

        String qrCode = generateQrCode(secret, appUser);
        recordMfaRequestSuccess(appUser.getId(), context);
        return new MfaSetupResponse(secret, qrCode);

    }

    @Override
    public MfaSetupConfirmResponse confirmMfaSetup(AuthRequestContext context, @NotNull @Length String code) {
        AppUser appUser = authCommonServicesImpl.findAuthenticatedUser();
        userStateValidator.validate(appUser, UserLifecycleOperation.MFA_SETUP_CONFIRM);

        if (appUser.isMfaEnabled()) {
            throw new BusinessException(ErrorCode.MFA_ALREADY_ENABLED, "MFA is already enabled for this account");
        }

        String secret = redisTemplate.opsForValue().get(MFA_SETUP_KEY_PREFIX + appUser.getId());
        if (secret == null) {
            throw new BusinessException(ErrorCode.MFA_SETUP_EXPIRED, "MFA setup session has expired. Please request again.");
        }

        isCodeValid(secret, code);

        appUser.setMfaEnabled(true);
        appUserRepository.save(appUser);
        redisTemplate.delete(MFA_SETUP_KEY_PREFIX + appUser.getId());

        recordMfaSetupConfirmSuccess(appUser.getId(), context);

        String[] recoveryCodes = new RecoveryCodeGenerator().generateCodes(10);
        //set recovery codes in db.
        recordCodeGenerationSuccess(appUser.getId(), context);
        return new MfaSetupConfirmResponse(recoveryCodes);
    }


    private void isCodeValid(String secret, @NotNull @Length String code) {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        if (!codeVerifier.isValidCode(secret, code)) {
            throw new BusinessException(ErrorCode.MFA_INVALID_CODE, "Invalid MFA code");
        }
    }

    private void recordMfaSetupConfirmSuccess(UUID id, AuthRequestContext context) {
        meterRegistry.counter("auth.mfa_setup.confirm.success").increment();
        auditEventLogger.log("auth.mfa_setup.confirm", id, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    private void recordCodeGenerationSuccess(UUID id, AuthRequestContext context) {
        meterRegistry.counter("auth.mfa_setup.confirm.code_generation").increment();
        auditEventLogger.log("auth.mfa_setup.confirm.code_generation", id, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    private void storeTemporaryMfaSecret(UUID id, String secret) {
        redisTemplate.opsForValue().set(MFA_SETUP_KEY_PREFIX + id,
                secret, Duration.ofMinutes(5));
    }

    private String generateQrCode(String secret, AppUser appUser) {

        QrData qrData = new QrData.Builder()
                .label("Dragon-of-North:" + appUser.getEmail())
                .secret(secret)
                .issuer("Dragon-of-North")
                .algorithm(SHA1)
                .digits(6)
                .period(30)
                .build();
        QrGenerator qrGenerator = new ZxingPngQrGenerator();

        try {
            byte[] imageData = qrGenerator.generate(qrData);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);

        } catch (QrGenerationException e) {
            log.error("Error generating QR code: {}", e.getMessage());
        }
        return null;
    }

    private void recordMfaRequestSuccess(UUID id, AuthRequestContext context) {
        meterRegistry.counter("auth.mfa_setup.request.success").increment();
        auditEventLogger.log("auth.mfa_setup.request", id, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }
}
