package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.util.TokenHasher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordlessLoginServiceImplTest {

    @InjectMocks
    private PasswordlessLoginServiceImpl passwordlessService;

    @Mock
    private AuthCommonServices authCommonServices;

    @Mock
    private UserStateValidator userStateValidator;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PasswordlessLoginEmailSender passwordlessLoginEmailSender;

    @Test
    void requestPasswordlessLogin_shouldStoreHashedTokenAndSendEmail() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");

        ReflectionTestUtils.setField(passwordlessService, "passwordlessTtlMinutes", 10L);
        ReflectionTestUtils.setField(passwordlessService, "passwordlessFrontendBaseUrl", "https://dragon.example");

        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenHasher.hashToken(anyString())).thenReturn("hashed-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        passwordlessService.requestPasswordlessLogin("user@example.com");

        verify(userStateValidator).validate(user, UserLifecycleOperation.PASSWORDLESS_LOGIN_REQUEST);
        verify(valueOperations).set(eq("auth:passwordless:token:hashed-token"), eq(userId.toString()), eq(Duration.ofMinutes(10)));
        verify(valueOperations).set(eq("auth:passwordless:user:" + userId), eq("hashed-token"), eq(Duration.ofMinutes(10)));
        verify(passwordlessLoginEmailSender).send(eq("user@example.com"), contains("/login/passwordless/verify?token="), eq(10L));
    }

    @Test
    void verifyPasswordlessLogin_shouldConsumeTokenAndCreateSession() {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setEmailVerified(true);
        user.setRoles(Set.of());

        AuthRequestContext context = new AuthRequestContext("device-1", "127.0.0.1", "req-1", "JUnit");
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(tokenHasher.hashToken("raw-token")).thenReturn("hashed-token");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:passwordless:token:hashed-token")).thenReturn(userId.toString());
        when(appUserRepository.findById(userId)).thenReturn(Optional.of(user));
        passwordlessService.verifyPasswordlessLogin("raw-token", context, response);

        verify(redisTemplate).delete("auth:passwordless:token:hashed-token");
        verify(redisTemplate).delete("auth:passwordless:user:" + userId);
        verify(userStateValidator).validate(user, UserLifecycleOperation.PASSWORDLESS_LOGIN_VERIFY);
        verify(authCommonServices).completeLogin(user, "user@example.com", response, context);
    }
}
