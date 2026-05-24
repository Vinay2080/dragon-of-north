package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.PasswordlessLoginService;

import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * Passwordless authentication implementation using one-time signed email links.
 * <p>
 * Request phase stores hashed token linkage in Redis and dispatches email; verification phase
 * validates token freshness and account state, then delegates to common session/MFA issuance.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class PasswordlessLoginServiceImpl implements PasswordlessLoginService {

    private static final String PASSWORDLESS_TOKEN_KEY_PREFIX = "auth:passwordless:token:";
    private static final String PASSWORDLESS_USER_KEY_PREFIX = "auth:passwordless:user:";


    private final AuthCommonServices authCommonServices;
    private final UserStateValidator userStateValidator;
    private final AppUserRepository appUserRepository;
    private final TokenHasher tokenHasher;
    private final StringRedisTemplate redisTemplate;
    private final PasswordlessLoginEmailSender passwordlessLoginEmailSender;

    @Value("${auth.passwordless.ttl-minutes:10}")
    private long passwordlessTtlMinutes;

    @Value("${auth.passwordless.frontend-base-url}")
    private String passwordlessFrontendBaseUrl;


    @Override
    public void requestPasswordlessLogin(String email) {

        appUserRepository.findByEmail(email).ifPresent(
                appUser -> {
                    userStateValidator.validate(appUser, UserLifecycleOperation.PASSWORDLESS_LOGIN_REQUEST);

                    String token = createToken();
                    String tokenHash = tokenHasher.hashToken(token);

                    storePasswordlessToken(appUser.getId(), tokenHash);
                    String link = buildPasswordlessLoginLink(token);
                    passwordlessLoginEmailSender.send(appUser.getEmail(), link, passwordlessTtlMinutes);
                });
    }

    @Override
    public MfaOrchestrationResult verifyPasswordlessLogin(String token, AuthRequestContext context, HttpServletResponse response) {
        AppUser appUser = verifyPasswordlessToken(token);
        userStateValidator.validate(appUser, UserLifecycleOperation.PASSWORDLESS_LOGIN_VERIFY);
        return authCommonServices.completeLogin(appUser, appUser.getEmail(), response, context);
    }


    private AppUser verifyPasswordlessToken(String token) {
        String tokenHash = tokenHasher.hashToken(token);
        String tokenKey = PASSWORDLESS_TOKEN_KEY_PREFIX + tokenHash;

        String userIdStr = redisTemplate.opsForValue().get(tokenKey);
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid or expired token");
        }

        redisTemplate.delete(tokenKey);
        redisTemplate.delete(PASSWORDLESS_USER_KEY_PREFIX + userIdStr);
        return appUserRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String createToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void storePasswordlessToken(UUID userId, String tokenHash) {
        String tokenKey = PASSWORDLESS_TOKEN_KEY_PREFIX + tokenHash;
        String userKey = PASSWORDLESS_USER_KEY_PREFIX + userId;

        // Ensure only one active passwordless token per user (latest wins).
        String previousTokenHash = redisTemplate.opsForValue().get(userKey);
        if (previousTokenHash != null && !previousTokenHash.isBlank()) {
            redisTemplate.delete(PASSWORDLESS_TOKEN_KEY_PREFIX + previousTokenHash);
        }

        Duration ttl = Duration.ofMinutes(passwordlessTtlMinutes);
        redisTemplate.opsForValue().set(tokenKey, userId.toString(), ttl);
        redisTemplate.opsForValue().set(userKey, tokenHash, ttl);
    }

    private String buildPasswordlessLoginLink(String token) {
        String baseUrl = passwordlessFrontendBaseUrl.endsWith("/")
                ? passwordlessFrontendBaseUrl.substring(0, passwordlessFrontendBaseUrl.length() - 1)
                : passwordlessFrontendBaseUrl;
        return String.format("%s/login/passwordless/verify?token=%s", baseUrl, token);
    }

}
