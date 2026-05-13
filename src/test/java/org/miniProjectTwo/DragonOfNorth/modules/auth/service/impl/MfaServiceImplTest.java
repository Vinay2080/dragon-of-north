package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupConfirmResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

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

        MfaSetupResponse response = mfaService.requestMfaSetup(context);

        assertNotNull(response.mfaSecret());
        assertFalse(response.mfaSecret().isBlank());
        assertTrue(response.mfaQrCode().startsWith("data:image/png;base64,"));
        verify(authCommonServices).findAuthenticatedUser();
        verify(userStateValidator).validate(user, UserLifecycleOperation.MFA_SETUP_REQUEST);
        verify(valueOperations).set(eq("auth:mfa:setup" + userId), eq(response.mfaSecret()), eq(Duration.ofMinutes(5)));
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
    void confirmMfaSetup_shouldEnableMfaWhenCodeIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");
        String secret = new DefaultSecretGenerator().generate();
        long timeStep = Math.floorDiv(new SystemTimeProvider().getTime(), 30);
        String code = new DefaultCodeGenerator().generate(secret, timeStep);

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:mfa:setup" + userId)).thenReturn(secret);
        when(meterRegistry.counter("auth.mfa_setup.confirm.success")).thenReturn(counter);
        when(meterRegistry.counter("auth.mfa_setup.confirm.code_generation")).thenReturn(counter);

        MfaSetupConfirmResponse response = mfaService.confirmMfaSetup(context, code);

        assertEquals(10, response.backupCodes().length);
        assertTrue(user.isMfaEnabled());
        verify(authCommonServices).findAuthenticatedUser();
        verify(userStateValidator).validate(user, UserLifecycleOperation.MFA_SETUP_CONFIRM);
        verify(appUserRepository).save(user);
        verify(redisTemplate).delete("auth:mfa:setup" + userId);
        verify(counter, times(2)).increment();
        verify(auditEventLogger).log("auth.mfa_setup.confirm", userId, "device-1", "127.0.0.1", "success", null, "req-1");
        verify(auditEventLogger).log("auth.mfa_setup.confirm.code_generation", userId, "device-1", "127.0.0.1", "success", null, "req-1");
    }

    @Test
    void confirmMfaSetup_shouldRejectInvalidCode() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");
        String secret = new DefaultSecretGenerator().generate();

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:mfa:setup" + userId)).thenReturn(secret);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> mfaService.confirmMfaSetup(context, "000000"));

        assertEquals(ErrorCode.MFA_INVALID_CODE, exception.getErrorCode());
        assertEquals("Invalid MFA code", exception.getMessage());
        verify(authCommonServices).findAuthenticatedUser();
        verify(userStateValidator).validate(user, UserLifecycleOperation.MFA_SETUP_CONFIRM);
        verify(appUserRepository, never()).save(any());
        verify(redisTemplate, never()).delete(anyString());
        verify(meterRegistry, never()).counter(anyString());
    }
}
