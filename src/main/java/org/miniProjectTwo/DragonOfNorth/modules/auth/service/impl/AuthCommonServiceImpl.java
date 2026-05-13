package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.security.service.impl.JwtServicesImpl;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.IdentifierNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

/**
 * Core authentication service handling login, token refresh, and user management.
 *
 * @see JwtServicesImpl for token operations
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AuthCommonServiceImpl implements AuthCommonServices {

    private final AuthenticationManager authenticationManager;
    private final JwtServices jwtServices;
    private final RoleRepository roleRepository;
    private final SessionService sessionService;
    private final MeterRegistry meterRegistry;
    private final AppUserRepository appUserRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final AuditEventLogger auditEventLogger;
    private final UserStateValidator userStateValidator;
    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Override
    public void login(String identifier, String password, HttpServletResponse response, AuthRequestContext context) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        UUID auditUserId = null;
        try {
            AppUser user = findUserForLogin(normalizedIdentifier);
            auditUserId = user.getId();

            userStateValidator.validate(user, UserLifecycleOperation.LOCAL_LOGIN);
            ensureLocalProvider(user);
            AppUser authenticatedUser = authenticateUser(normalizedIdentifier, password);
            ensureIdentifierVerified(user, normalizedIdentifier);

            LoginTokens loginTokens = generateLoginTokens(authenticatedUser);
            persistLoginSession(authenticatedUser, loginTokens.refreshToken(), context);
            writeAuthCookies(response, loginTokens);

            recordLoginSuccess(authenticatedUser.getId(), context);
        } catch (AuthenticationException | BusinessException exception) {
            recordLoginFailure(auditUserId, context, exception.getMessage());
            throw exception;
        }
    }

    @Override
    public void refreshToken(String oldRefreshToken, HttpServletResponse response, AuthRequestContext context) {
        try {
            validateRefreshRequest(oldRefreshToken, context);
            TokenRefreshData refreshData = rotateRefreshTokens(oldRefreshToken, context.deviceId());
            writeAuthCookies(response, refreshData.toLoginTokens());
            recordRefreshSuccess(refreshData.userId(), context);
        } catch (BusinessException exception) {
            clearRefreshTokenCookie(response);
            recordRefreshFailure(context, exception.getMessage());
            throw exception;
        }
    }

    @Override
    public void assignDefaultRole(AppUser appUser) {
        if (!appUser.hasAnyRoles()) {
            Role userRole = roleRepository.findByRoleName(RoleName.USER)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, RoleName.USER.toString()));
            appUser.getRoles().add(userRole);
        }
    }

    @Override
    public void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser) {
        appUser.setAppUserStatus(appUserStatus);
    }

    @Override
    public void logoutUser(String refreshToken, HttpServletResponse response, AuthRequestContext context) {
        validateLogoutRequest(refreshToken, context);

        UUID userId = extractLogoutUserId(refreshToken, context);
        try {
            userStateValidator.validate(findUserById(userId), UserLifecycleOperation.SESSION_REVOKE_CURRENT);
            sessionService.revokeSession(refreshToken, context.deviceId());
        } catch (BusinessException exception) {
            recordLogoutFailure(userId, context, exception.getMessage());
            clearAuthCookies(response);
            return;
        }

        clearAuthCookies(response);
        recordLogoutSuccess(userId, context);
    }

    @Override
    @Transactional
    public void deleteAccount(HttpServletResponse response, AuthRequestContext context) {
        AppUser appUser = findAuthenticatedUser();
        userStateValidator.validate(appUser, UserLifecycleOperation.ACCOUNT_DELETION);
        appUser.setAppUserStatus(AppUserStatus.DELETED);
        appUserRepository.save(appUser);
        sessionService.revokeAllSessionsByUserId(appUser.getId());
        clearAuthCookies(response);
        recordAccountDeletionSuccess(appUser.getId(), context);
    }
    private void recordAccountDeletionSuccess(UUID userId, AuthRequestContext context) {
        meterRegistry.counter("auth.account_delete.success").increment();
        auditEventLogger.log("auth.account_delete", userId, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    @Override
    public AppUser findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "User not authenticated");
        }

        UUID userId = resolveAuthenticatedUserId(authentication.getPrincipal());
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public void completeLogin(AppUser appUser, String identifier, HttpServletResponse response, AuthRequestContext context) {
        ensureIdentifierVerified(appUser, identifier);
        LoginTokens loginTokens = generateLoginTokens(appUser);
        persistLoginSession(appUser, loginTokens.refreshToken(), context);
        writeAuthCookies(response, loginTokens);
        recordLoginSuccess(appUser.getId(), context);
    }

    private UUID resolveAuthenticatedUserId(Object principal) {
        return switch (principal) {
            case AppUserDetails appUserDetails -> appUserDetails.getAppUser().getId();
            case UUID id -> id;
            case String raw when !raw.isBlank() -> parseAuthenticatedUserId(raw);
            case null, default -> throw new BusinessException(ErrorCode.ACCESS_DENIED, "User not authenticated");
        };
    }

    private UUID parseAuthenticatedUserId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Invalid authentication principal");
        }
    }


    private void validateLogoutRequest(String refreshToken, AuthRequestContext context) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            recordLogoutFailure(null, context, "refresh token missing");
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "refresh token missing");
        }
        if (context.deviceId() == null || context.deviceId().trim().isEmpty()) {
            recordLogoutFailure(null, context, "device ID missing");
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "device ID missing");
        }
    }

    private UUID extractLogoutUserId(String refreshToken, AuthRequestContext context) {
        try {
            return jwtServices.extractUserId(refreshToken);
        } catch (BusinessException businessException) {
            recordLogoutFailure(null, context, "invalid refresh token: " + businessException.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token");
        }
    }

    private void recordLogoutFailure(UUID userId, AuthRequestContext context, String message) {
        meterRegistry.counter("auth.logout.failure").increment();
        auditEventLogger.log("auth.logout", userId, context.deviceId(), context.ipAddress(), "failure", message, context.requestId());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        clearRefreshTokenCookie(response);
        clearAccessTokenCookie(response);
    }

    private void recordLogoutSuccess(UUID userId, AuthRequestContext context) {
        meterRegistry.counter("auth.logout.success").increment();
        auditEventLogger.log("auth.logout", userId, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    private void validateRefreshRequest(String oldRefreshToken, AuthRequestContext context) {
        if (oldRefreshToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token missing");
        }
        ensureDeviceIdPresent(context.deviceId());
    }

    private AppUser findUserById(UUID userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private TokenRefreshData rotateRefreshTokens(String oldRefreshToken, String deviceId) {
        UUID userIdFromOldToken = jwtServices.extractUserId(oldRefreshToken);
        userStateValidator.validate(findUserById(userIdFromOldToken), UserLifecycleOperation.SESSION_ROTATE_REFRESH);
        String newRefreshToken = jwtServices.generateRefreshToken(userIdFromOldToken);
        UUID userId = sessionService.validateAndRotateSession(oldRefreshToken, newRefreshToken, deviceId);
        Set<Role> roles = roleRepository.findRolesById(userId);
        String newAccessToken = jwtServices.refreshAccessToken(newRefreshToken, roles);
        return new TokenRefreshData(userId, newAccessToken, newRefreshToken);
    }

    private void recordRefreshSuccess(UUID userId, AuthRequestContext context) {
        meterRegistry.counter("auth.refresh.success").increment();
        auditEventLogger.log("auth.refresh", userId, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    private void recordRefreshFailure(AuthRequestContext context, String message) {
        meterRegistry.counter("auth.refresh.failure").increment();
        auditEventLogger.log("auth.refresh", null, context.deviceId(), context.ipAddress(), "failure", message, context.requestId());
    }

    private void ensureDeviceIdPresent(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "device ID missing");
        }
    }

    private AppUser findUserForLogin(String identifier) {
        return identifier.contains("@")
                ? appUserRepository.findByEmail(identifier)
                  .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED))
                : appUserRepository.findByPhone(identifier)
                  .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED));
    }

    private void ensureLocalProvider(AppUser user) {
        if (!userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), LOCAL)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Account registered via Google. Use Google login.");
        }
    }

    private AppUser authenticateUser(String identifier, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, password)
        );

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Authentication principal is null");
        }
        if (!(principal instanceof AppUserDetails appUserDetails)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid principal type");
        }

        return appUserDetails.getAppUser();
    }

    public void ensureIdentifierVerified(AppUser user, String identifier) {
        if (identifier.contains("@") && !user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "Email not verified. Please verify your email before logging in.");
        }
        if (!identifier.contains("@") && !user.isPhoneNumberVerified()) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED, "Phone number not verified. Please verify your phone before logging in.");
        }
    }

    private String normalizeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return identifier.contains("@")
                ? IdentifierNormalizer.normalizeEmail(identifier)
                : IdentifierNormalizer.normalizePhone(identifier);
    }

    public LoginTokens generateLoginTokens(AppUser appUser) {
        String accessToken = jwtServices.generateAccessToken(appUser.getId(), appUser.getRoles());
        String refreshToken = jwtServices.generateRefreshToken(appUser.getId());
        return new LoginTokens(accessToken, refreshToken);
    }

    public void persistLoginSession(AppUser appUser, String refreshToken, AuthRequestContext context) {
        sessionService.createSession(appUser, refreshToken, context.ipAddress(), context.deviceId(), context.userAgent());
    }

    public void writeAuthCookies(HttpServletResponse response, LoginTokens loginTokens) {
        setAccessToken(response, loginTokens.accessToken());
        setRefreshToken(response, loginTokens.refreshToken());
    }

    public void recordLoginSuccess(UUID userId, AuthRequestContext context) {
        meterRegistry.counter("auth.login.success").increment();
        auditEventLogger.log("auth.login", userId, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    private void recordLoginFailure(UUID userId, AuthRequestContext context, String message) {
        meterRegistry.counter("auth.login.failure").increment();
        auditEventLogger.log("auth.login", userId, context.deviceId(), context.ipAddress(), "failure", message, context.requestId());
    }

    public void setAccessToken(HttpServletResponse response, String token) {
        Cookie accessCookie = new Cookie("access_token", token);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(cookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 15);
        accessCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(accessCookie);
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(cookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        accessCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(accessCookie);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie refrehCookie = new Cookie("refresh_token", "");
        refrehCookie.setHttpOnly(true);
        refrehCookie.setSecure(cookieSecure);
        refrehCookie.setPath("/");
        refrehCookie.setMaxAge(0);
        refrehCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(refrehCookie);
    }

    public void setRefreshToken(HttpServletResponse response, String token) {
        Cookie refreshCookie = new Cookie("refresh_token", token);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
        refreshCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(refreshCookie);
    }

    public record LoginTokens(String accessToken, String refreshToken) {
    }

    private record TokenRefreshData(UUID userId, String accessToken, String refreshToken) {
        private LoginTokens toLoginTokens() {
            return new LoginTokens(accessToken, refreshToken);
        }
    }
}
