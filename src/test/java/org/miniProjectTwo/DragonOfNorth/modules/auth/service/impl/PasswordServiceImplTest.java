package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordChangeRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceImplTest {

    @InjectMocks
    private PasswordServiceImpl passwordService;

    @Mock
    private OtpService otpService;

    @Mock
    private SessionService sessionService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditEventLogger auditEventLogger;

    @Mock
    private UserStateValidator userStateValidator;

    @Mock
    private AuthCommonServices authCommonServices;

    @Mock
    private Counter counter;

    @Test
    void changePassword_shouldRejectGoogleOnlyAccounts() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setPassword("encoded-password");

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> passwordService.changePassword(new PasswordChangeRequest("Old@12345", "New@12345")));

        assertEquals(ErrorCode.PASSWORD_CHANGE_NOT_ALLOWED, exception.getErrorCode());
        assertEquals("Password change not allowed for Google accounts", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(appUserRepository, never()).save(any());
        verify(sessionService, never()).revokeAllSessionsByUserId(any());
    }

    @Test
    void changePassword_shouldRejectIncorrectCurrentPassword() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setPassword("encoded-password");

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(true);
        when(passwordEncoder.matches("Wrong@123", "encoded-password")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> passwordService.changePassword(new PasswordChangeRequest("Wrong@123", "New@12345")));

        assertEquals(ErrorCode.INVALID_CURRENT_PASSWORD, exception.getErrorCode());
        assertEquals("Current password is incorrect", exception.getMessage());
        verify(appUserRepository, never()).save(any());
        verify(sessionService, never()).revokeAllSessionsByUserId(any());
    }

    @Test
    void changePassword_shouldRejectSamePasswordAsCurrentPassword() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setPassword("encoded-password");

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(true);
        when(passwordEncoder.matches("Same@1234", "encoded-password")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> passwordService.changePassword(new PasswordChangeRequest("Same@1234", "Same@1234")));

        assertEquals(ErrorCode.SAME_PASSWORD, exception.getErrorCode());
        assertEquals("New password must be different from current password", exception.getMessage());
        verify(appUserRepository, never()).save(any());
        verify(sessionService, never()).revokeAllSessionsByUserId(any());
    }

    @Test
    void changePassword_shouldUpdatePasswordAndRevokeSessions_whenValid() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setPassword("encoded-old");

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(true);
        when(passwordEncoder.matches("Old@12345", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("New@12345")).thenReturn("encoded-new");
        when(meterRegistry.counter(anyString())).thenReturn(counter);

        passwordService.changePassword(new PasswordChangeRequest("Old@12345", "New@12345"));

        verify(userStateValidator).validate(user, UserLifecycleOperation.PASSWORD_CHANGE);
        verify(appUserRepository).save(user);
        verify(sessionService).revokeAllSessionsByUserId(userId);
        verify(meterRegistry).counter("auth.password_change.success");
        verify(counter).increment();
        verify(auditEventLogger).log(eq("auth.password_change"), eq(userId), isNull(), isNull(), eq("success"), isNull(), isNull());
        assertEquals("encoded-new", user.getPassword());
    }
}

