package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupConfirmResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.service.MfaVerificationService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaSettings;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserMfaSettingsRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.MfaRecoveryCodeService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.TotpService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.encryption.EncryptionService;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceImplTest {

    @Mock
    private AuthCommonServices authCommonServices;

    @Mock
    private UserStateValidator userStateValidator;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AuditEventLogger auditEventLogger;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private Counter counter;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private UserMfaSettingsRepository userMfaSettingsRepository;

    @Mock
    private TotpService totpService;

    @Mock
    private MfaRecoveryCodeService recoveryCodeService;

    @Mock
    private MfaVerificationService mfaVerificationService;

    @InjectMocks
    private MfaServiceImpl mfaService;

    @Test
    void requestMfaSetup_shouldPersistSecretAndReturnQrCode() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(meterRegistry.counter("auth.mfa_setup.request.success")).thenReturn(counter);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(totpService.generateSecret()).thenReturn("setup-secret");
        when(encryptionService.encrypt("setup-secret")).thenReturn("encrypted-setup-secret");
        when(totpService.generateQrCode("setup-secret", user)).thenReturn("data:image/png;base64,AAAA");

        MfaSetupResponse response = mfaService.requestMfaSetup(context);

        assertEquals("setup-secret", response.mfaSecret());
        assertTrue(response.mfaQrCode().startsWith("data:image/png;base64,"));
        verify(authCommonServices).findAuthenticatedUser();
        verify(userStateValidator).validate(user, UserLifecycleOperation.MFA_SETUP_REQUEST);
        verify(valueOperations).set(eq("auth:mfa:setup" + userId), eq("encrypted-setup-secret"), eq(Duration.ofMinutes(5)));
        verify(counter).increment();
        verify(auditEventLogger).log("auth.mfa_setup.request", userId, "device-1", "127.0.0.1", "success", null, "req-1");
    }

    @Test
    void requestMfaSetup_shouldRejectWhenMfaAlreadyEnabled() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setMfaEnabled(true);

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class, () -> mfaService.requestMfaSetup(context));

        assertEquals(ErrorCode.MFA_ALREADY_ENABLED, exception.getErrorCode());
        assertEquals("MFA is already enabled for this account", exception.getMessage());
        verify(authCommonServices).findAuthenticatedUser();
        verify(userStateValidator).validate(user, UserLifecycleOperation.MFA_SETUP_REQUEST);
        verify(redisTemplate, never()).opsForValue();
        verify(meterRegistry, never()).counter(anyString());
    }

    @Test
    void confirmMfaSetup_shouldEnableMfaWhenCodeIsValid() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:mfa:setup" + userId)).thenReturn("encrypted-setup-secret");
        when(encryptionService.decrypt("encrypted-setup-secret")).thenReturn("setup-secret");
        when(totpService.isValidCode("setup-secret", "123456")).thenReturn(true);
        when(userMfaSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userMfaSettingsRepository.save(any(UserMfaSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recoveryCodeService.generateAndStoreRecoveryCodes(any(UserMfaSettings.class)))
                .thenReturn(new String[]{"code-1", "code-2"});
        when(meterRegistry.counter("auth.mfa_setup.confirm.success")).thenReturn(counter);

        MfaSetupConfirmResponse response = mfaService.confirmMfaSetup(context, "123456");

        assertArrayEquals(new String[]{"code-1", "code-2"}, response.backupCodes());
        assertTrue(user.isMfaEnabled());
        assertNotNull(user.getMfaEnabledAt());
        verify(authCommonServices).findAuthenticatedUser();
        verify(userStateValidator).validate(user, UserLifecycleOperation.MFA_SETUP_CONFIRM);
        verify(userMfaSettingsRepository).save(argThat(settings ->
                settings.getUser() == user
                        && "encrypted-setup-secret".equals(settings.getTotpSecretEncrypted())
                        && settings.getTotpEnabledAt() != null
        ));
        verify(appUserRepository).save(user);
        verify(recoveryCodeService).generateAndStoreRecoveryCodes(any(UserMfaSettings.class));
        verify(counter).increment();
        verify(auditEventLogger).log("auth.mfa_setup.confirm", userId, "device-1", "127.0.0.1", "success", null, "req-1");
    }

    @Test
    void confirmMfaSetup_shouldRejectInvalidCode() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:mfa:setup" + userId)).thenReturn("encrypted-setup-secret");
        when(encryptionService.decrypt("encrypted-setup-secret")).thenReturn("setup-secret");
        when(totpService.isValidCode("setup-secret", "000000")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> mfaService.confirmMfaSetup(context, "000000"));

        assertEquals(ErrorCode.MFA_INVALID_CODE, exception.getErrorCode());
        assertEquals("Invalid MFA code", exception.getMessage());
        verify(authCommonServices).findAuthenticatedUser();
        verify(userStateValidator).validate(user, UserLifecycleOperation.MFA_SETUP_CONFIRM);
        verify(appUserRepository, never()).save(any());
        verify(userMfaSettingsRepository, never()).save(any());
        verify(meterRegistry, never()).counter(anyString());
    }

    @Test
    void confirmMfaSetup_shouldAllowExactlyOneWinnerUnderConcurrentConsume() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");
        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        AtomicBoolean consumed = new AtomicBoolean(false);
        when(valueOperations.getAndDelete("auth:mfa:setup" + userId))
                .thenAnswer(invocation -> consumed.compareAndSet(false, true) ? "encrypted-setup-secret" : null);
        when(encryptionService.decrypt("encrypted-setup-secret")).thenReturn("setup-secret");
        when(totpService.isValidCode("setup-secret", "123456")).thenReturn(true);
        when(userMfaSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userMfaSettingsRepository.save(any(UserMfaSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recoveryCodeService.generateAndStoreRecoveryCodes(any(UserMfaSettings.class))).thenReturn(new String[]{"code-1"});
        when(meterRegistry.counter("auth.mfa_setup.confirm.success")).thenReturn(counter);

        int parallel = 6;
        ExecutorService pool = Executors.newFixedThreadPool(parallel);
        CountDownLatch ready = new CountDownLatch(parallel);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);
        try {
            List<Future<Boolean>> futures = java.util.stream.IntStream.range(0, parallel)
                    .mapToObj(i -> pool.submit(() -> {
                        ready.countDown();
                        start.await(5, TimeUnit.SECONDS);
                        try {
                            mfaService.confirmMfaSetup(context, "123456");
                            return true;
                        } catch (BusinessException ex) {
                            return false;
                        }
                    }))
                    .toList();
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            for (Future<Boolean> f : futures) {
                if (f.get(5, TimeUnit.SECONDS)) successes.incrementAndGet();
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(1, successes.get());
    }

    @Test
    void verifyTotpCode_shouldDecryptPersistedSecretBeforeValidation() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setMfaEnabled(true);

        UserMfaSettings settings = new UserMfaSettings();
        settings.setTotpSecretEncrypted("encrypted-persisted-secret");

        when(mfaVerificationService.verifyAtLogin(eq(user), eq(ProviderType.TOTP), eq("123456"), any()))
                .thenReturn(VerifyResult.success(ProviderType.TOTP));

        assertTrue(mfaService.verifyTotpCode(user, "123456"));
    }

    @Test
    void verifyRecoveryCode_shouldConsumePersistedRecoveryCode() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setMfaEnabled(true);

        when(mfaVerificationService.verifyAtLogin(eq(user), eq(ProviderType.RECOVERY_CODE), eq("ABCD-EFGH"), any()))
                .thenReturn(VerifyResult.success(ProviderType.RECOVERY_CODE));

        assertTrue(mfaService.verifyRecoveryCode(user, "ABCD-EFGH"));
    }

    @Test
    void verifyMfaCode_shouldAcceptRecoveryCodeWhenTotpFails() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setMfaEnabled(true);

        UserMfaSettings settings = new UserMfaSettings();
        settings.setTotpSecretEncrypted("encrypted-persisted-secret");

        when(mfaVerificationService.verifyAtLogin(eq(user), eq(ProviderType.TOTP), eq("ABCD-EFGH"), any()))
                .thenReturn(VerifyResult.failure(ProviderType.TOTP, "invalid_code"));
        when(mfaVerificationService.verifyAtLogin(eq(user), eq(ProviderType.RECOVERY_CODE), eq("ABCD-EFGH"), any()))
                .thenReturn(VerifyResult.success(ProviderType.RECOVERY_CODE));

        assertTrue(mfaService.verifyMfaCode(user, "ABCD-EFGH"));
    }
}
//todo warning