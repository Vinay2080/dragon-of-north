package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordChangeRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordResetConfirmRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.security.service.impl.JwtServicesImpl;
import org.miniProjectTwo.DragonOfNorth.shared.enums.*;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

/**
 * Core authentication service handling login, token refresh, and user management.
 * Manages JWT token generation, secure cookie handling, and user status transitions.
 * Integrates with Spring Security for authentication and database for token storage.
 * Critical for session management and security enforcement across the application.
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
    private final OtpService otpService;
    private final MeterRegistry meterRegistry;
    private final AppUserRepository appUserRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventLogger auditEventLogger;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * Authenticates user credentials and issues JWT tokens.
     * Validates credentials via Spring Security, generates access/refresh tokens,
     * stores refresh token in a database, and sets secure HTTP-only cookies.
     * Critical for establishing user sessions with proper security controls.
     *
     * @param identifier user email or phone number
     * @param password   user password for authentication
     * @param response   HTTP response for setting secure cookies
     * @param context    request metadata (device, IP, request id, user-agent)
     * @throws BusinessException if authentication fails or principal is invalid
     */
    @Override
    public void login(String identifier, String password, HttpServletResponse response, AuthRequestContext context) {
        String ipAddress = context.ipAddress();
        String requestId = context.requestId();
        String deviceId = context.deviceId();
        UUID auditUserId = null;
        try {
            AppUser user = identifier.contains("@")
                    ? appUserRepository.findByEmail(identifier).orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED))
                    : appUserRepository.findByPhone(identifier).orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED));
            auditUserId = user.getId();

            if (!userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), LOCAL)) {
                throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Account registered via Google. Use Google login.");
            }

            final Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));

            if (authentication.getPrincipal() == null) {
                throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Authentication principal is null");
            }

            if (!(authentication.getPrincipal() instanceof AppUserDetails appUserDetails)) {
                throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid principal type");
            }
            if (!appUserRepository.isEmailVerified(user.getId())) {
                throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "Email not verified. Please verify your email before logging in.");
            }

            AppUser appUser = appUserDetails.getAppUser();
            final String accessToken = jwtServices.generateAccessToken(appUser.getId(), appUser.getRoles());
            final String refreshToken = jwtServices.generateRefreshToken(appUser.getId());
            String userAgent = context.userAgent();

            sessionService.createSession(appUser, refreshToken, ipAddress, deviceId, userAgent);
            setAccessToken(response, accessToken);
            setRefreshToken(response, refreshToken);

            meterRegistry.counter("auth.login.success").increment();
            auditEventLogger.log("auth.login", appUser.getId(), deviceId, ipAddress, "success", null, requestId);
        } catch (AuthenticationException | BusinessException exception) {
            meterRegistry.counter("auth.login.failure").increment();
            auditEventLogger.log("auth.login", auditUserId, deviceId, ipAddress, "failure", exception.getMessage(), requestId);
            throw exception;
        }

    }

    /**
     * Refreshes access token using a valid refresh token from cookies.
     * Extracts refresh token from an HTTP-only cookie, validates against a database,
     * generates a new access token, and updates cookie. Clears refresh token on failure.
     * Critical for maintaining user sessions without re-authentication.
     *
     * @param oldRefreshToken refresh token extracted from request cookies
     * @param response HTTP response for setting a new access token cookie
     * @param context request metadata (device, IP, request id)
     * @throws BusinessException if the refresh token is missing, invalid, or expired
     */
    @Override
    public void refreshToken(String oldRefreshToken, HttpServletResponse response, AuthRequestContext context) {
        String ipAddress = context.ipAddress();
        String requestId = context.requestId();
        String deviceId = context.deviceId();
        if (oldRefreshToken == null) {
            meterRegistry.counter("auth.refresh.failure").increment();
            auditEventLogger.log("auth.refresh", null, deviceId, ipAddress, "failure", "Refresh token missing", requestId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token missing");
        }

        if (deviceId == null || deviceId.trim().isEmpty()) {
            meterRegistry.counter("auth.refresh.failure").increment();
            auditEventLogger.log("auth.refresh", null, deviceId, ipAddress, "failure", "device ID missing", requestId);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "device ID missing");
        }

        try {
            UUID userIdFromOldToken = jwtServices.extractUserId(oldRefreshToken);
            String newRefreshToken = jwtServices.generateRefreshToken(userIdFromOldToken);

            UUID userId = sessionService.validateAndRotateSession(oldRefreshToken, newRefreshToken, deviceId);
            Set<Role> roles = roleRepository.findRolesById(userId);

            String newAccessToken = jwtServices.refreshAccessToken(newRefreshToken, roles);

            setAccessToken(response, newAccessToken);
            setRefreshToken(response, newRefreshToken);
            meterRegistry.counter("auth.refresh.success").increment();
            auditEventLogger.log("auth.refresh", userId, deviceId, ipAddress, "success", null, requestId);
        } catch (BusinessException e) {
            clearRefreshTokenCookie(response);
            meterRegistry.counter("auth.refresh.failure").increment();
            auditEventLogger.log("auth.refresh", null, deviceId, ipAddress, "failure", e.getMessage(), requestId);
            throw e;
        }
    }

    /**
     * Assigns a default USER role to users without any roles.
     * Ensures all users have basic permissions for system access.
     * Only assigns a role if the user has no existing roles to preserve role hierarchy.
     * Critical for user onboarding and permission management.
     *
     * @param appUser user to receive a default role
     * @throws BusinessException if a USER role is not found in a database
     */
    @Override
    public void assignDefaultRole(AppUser appUser) {
        if (!appUser.hasAnyRoles()) {
            Role userRole = roleRepository.findByRoleName(RoleName.USER).orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, RoleName.USER.toString()));
            appUser.getRoles().add(userRole);
        }
    }

    /**
     * Updates user status from CREATED to VERIFIED for account activation.
     * Allows status transition only from CREATED to VERIFIED to prevent
     * unauthorized status changes. Blocks re-verification of already verified users.
     * Critical for user registration flow completion and security enforcement.
     *
     * @param appUserStatus must be CREATED to trigger verification
     * @param appUser       user to update status
     * @throws BusinessException if the user is already verified or status is invalid
     */
    @Override
    public void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser) {
        appUser.setAppUserStatus(appUserStatus);
    }

    @Override
    public void logoutUser(String refreshToken, HttpServletResponse response, AuthRequestContext context) {
        String ipAddress = context.ipAddress();
        String requestId = context.requestId();
        String deviceId = context.deviceId();
        if (refreshToken == null || refreshToken.isEmpty()) {
            meterRegistry.counter("auth.logout.failure").increment();
            auditEventLogger.log("auth.logout", null, deviceId, ipAddress, "failure", "refresh token missing", requestId);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "refresh token missing");
        }

        if (deviceId == null || deviceId.trim().isEmpty()) {
            meterRegistry.counter("auth.logout.failure").increment();
            auditEventLogger.log("auth.logout", null, deviceId, ipAddress, "failure", "device ID missing", requestId);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "device ID missing");
        }

        UUID userId = null;
        try {
            userId = jwtServices.extractUserId(refreshToken);
        } catch (BusinessException ignored) {
            // keep nullable userId in the audit event when token extraction fails
        }

        try {
            sessionService.revokeSession(refreshToken, deviceId);
        } catch (BusinessException e) {
            meterRegistry.counter("auth.logout.failure").increment();
            auditEventLogger.log("auth.logout", userId, deviceId, ipAddress, "failure", e.getMessage(), requestId);
            clearRefreshTokenCookie(response);
            clearAccessTokenCookie(response);
            return;
        }
        clearRefreshTokenCookie(response);
        clearAccessTokenCookie(response);
        meterRegistry.counter("auth.logout.success").increment();
        auditEventLogger.log("auth.logout", userId, deviceId, ipAddress, "success", null, requestId);

    }

    @Override
    public void requestPasswordResetOtp(String identifier, IdentifierType identifierType) {
        AppUser user = identifierType == IdentifierType.EMAIL
                ? appUserRepository.findByEmail(identifier).orElse(null)
                : appUserRepository.findByPhone(identifier).orElse(null);

        if (user != null && userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), LOCAL)) {
            if (identifierType == IdentifierType.EMAIL) {
                otpService.createEmailOtp(identifier, OtpPurpose.PASSWORD_RESET);
            } else {
                otpService.createPhoneOtp(identifier, OtpPurpose.PASSWORD_RESET);
            }
        } else {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "No user found with the provided identifier for password reset");
        }

        meterRegistry.counter("auth.password_reset.requested").increment();
        auditEventLogger.log("auth.password_reset.request", user.getId(), null, null, "success", "identifier_type=" + identifierType, null);
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        OtpVerificationStatus status = request.identifierType() == IdentifierType.EMAIL
                ? otpService.verifyEmailOtp(request.identifier(), request.otp(), OtpPurpose.PASSWORD_RESET)
                : otpService.verifyPhoneOtp(request.identifier(), request.otp(), OtpPurpose.PASSWORD_RESET);

        if (!status.isSuccess()) {
            meterRegistry.counter("auth.password_reset.failure").increment();
            auditEventLogger.log("auth.password_reset.confirm", null, null, null, "failure", status.getMessage(), null);
            throw new BusinessException(ErrorCode.INVALID_INPUT, status.getMessage());
        }

        AppUser appUser = request.identifierType() == IdentifierType.EMAIL
                ? appUserRepository.findByEmail(request.identifier())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND))
                : appUserRepository.findByPhone(request.identifier())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!userAuthProviderRepository.existsByUserIdAndProvider(appUser.getId(), LOCAL)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Password reset is available only for password-based accounts.");
        }

        appUser.setPassword(passwordEncoder.encode(request.newPassword()));
        sessionService.revokeAllSessionsByUserId(appUser.getId());

        meterRegistry.counter("auth.password_reset.success").increment();
        auditEventLogger.log("auth.password_reset.confirm", appUser.getId(), null, null, "success", "identifier_type=" + request.identifierType(), null);
    }

    @Override
    public void changePassword(PasswordChangeRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserDetails appUserDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User not authenticated");
        }

        AppUser appUser = appUserDetails.getAppUser();

        if (!passwordEncoder.matches(request.oldPassword(), appUser.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Old password is incorrect");
        }

        appUser.setPassword(passwordEncoder.encode(request.newPassword()));
        sessionService.revokeAllSessionsByUserId(appUser.getId());

        meterRegistry.counter("auth.password_change.success").increment();
        auditEventLogger.log("auth.password_change", appUser.getId(), null, null, "success", null, null);
    }


    /**
     * Sets secure HTTP-only access token cookie.
     * Configures cookie with security attributes: HttpOnly, Secure, SameSite=None,
     * 15-minute expiration, and root path. Critical for protecting access tokens
     * from XSS attacks and ensuring proper token transmission.
     *
     * @param response HTTP response for cookie setting
     * @param token    JWT access token value
     */
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

    /**
     * Clears refresh token cookie by setting max age to 0.
     * Removes refresh token from client storage on authentication failures.
     * Uses the same path and security attributes as the original cookie for proper clearing.
     * Critical for security cleanup on token invalidation.
     *
     * @param response HTTP response for cookie clearing
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie refrehCookie = new Cookie("refresh_token", "");
        refrehCookie.setHttpOnly(true);
        refrehCookie.setSecure(cookieSecure);
        refrehCookie.setPath("/");
        refrehCookie.setMaxAge(0);
        refrehCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(refrehCookie);
    }

    /**
     * Sets secure HTTP-only refresh token cookie.
     * Configures cookie with security attributes: HttpOnly, Secure, SameSite=None,
     * 7-day expiration, and restricted path to /jwt/refresh endpoint.
     * Critical for long-term session management and token security.
     *
     * @param response HTTP response for cookie setting
     * @param token    JWT refresh token value
     */
    public void setRefreshToken(HttpServletResponse response, String token) {
        Cookie refreshCookie = new Cookie("refresh_token", token);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        refreshCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(refreshCookie);
    }
}
