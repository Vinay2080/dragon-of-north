package org.miniProjectTwo.DragonOfNorth.impl.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.config.security.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.config.security.JwtServices;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.impl.RefreshTokenService;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.VERIFIED;

/**
 * Core authentication service handling login, token refresh, and user management.
 * Manages JWT token generation, secure cookie handling, and user status transitions.
 * Integrates with Spring Security for authentication and database for token storage.
 * Critical for session management and security enforcement across the application.
 *
 * @see JwtServices for token operations
 * @see RefreshTokenService for token persistence
 */
@RequiredArgsConstructor
@Service
public class AuthCommonServiceImpl implements AuthCommonServices {

    private final AuthenticationManager authenticationManager;
    private final JwtServices jwtServices;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * Authenticates user credentials and issues JWT tokens.
     * Validates credentials via Spring Security, generates access/refresh tokens,
     * stores refresh token in a database, and sets secure HTTP-only cookies.
     * Critical for establishing user sessions with proper security controls.
     *
     * @param identifier user email or phone number
     * @param password   user password for authentication
     * @param response   HTTP response for setting secure cookies
     * @throws BusinessException if authentication fails or principal is invalid
     */
    @Override
    public void login(String identifier, String password, HttpServletResponse response) {
        final Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));

        if (authentication.getPrincipal() == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Authentication principal is null");
        }

        if (!(authentication.getPrincipal() instanceof AppUserDetails appUserDetails)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid principal type");
        }

        AppUser appUser = appUserDetails.getAppUser();

        final String accessToken = jwtServices.generateAccessToken(appUser.getId(), appUser.getRoles());
        final String refreshToken = jwtServices.generateRefreshToken(appUser.getId());

        refreshTokenService.storeRefreshToken(appUser, refreshToken);

        setAccessToken(response, accessToken);
        setRefreshCookie(response, refreshToken);

    }

    /**
     * Refreshes access token using a valid refresh token from cookies.
     * Extracts refresh token from an HTTP-only cookie, validates against a database,
     * generates a new access token, and updates cookie. Clears refresh token on failure.
     * Critical for maintaining user sessions without re-authentication.
     *
     * @param request  HTTP request containing refresh token cookie
     * @param response HTTP response for setting a new access token cookie
     * @throws BusinessException if the refresh token is missing, invalid, or expired
     */
    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshToken(request);
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token missing");
        }

        try {

            refreshTokenService.verifyAndUpdateToken(refreshToken);
            UUID userId = jwtServices.extractUserId(refreshToken);
            Set<Role> roles = appUserRepository.findRolesById(userId);

            String newAccessToken = jwtServices.refreshAccessToken(refreshToken, roles);

            setAccessToken(response, newAccessToken);

        } catch (BusinessException e) {
            clearRefreshTokenCookie(response);
            throw e;
        }
    }

    /**
     * Assigns a default USER role to users without any roles.
     * Ensures all users have basic permissions for system access.
     * Only assigns a role if the user has no existing roles to preserve role hierarchy.
     * Critical for user onboarding and permission management.
     *
     * @param appUser user to receive default role
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
     * @param appUser user to update status
     * @throws BusinessException if the user is already verified or status is invalid
     */
    @Override
    public void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser) {
        if (appUser.getAppUserStatus() == VERIFIED) {
            throw new BusinessException(ErrorCode.USER_ALREADY_VERIFIED);
        }

        if (appUserStatus == CREATED) {
            appUser.setAppUserStatus(VERIFIED);
        } else {
            throw new BusinessException(ErrorCode.STATUS_MISMATCH, CREATED.toString());
        }
    }

    /**
     * Extracts refresh token from HTTP-only cookies.
     * Searches a cookie array for 'refresh_token' cookie name.
     * Returns null if no cookies exist or a refresh token is not found.
     * Critical for token refresh flow security.
     *
     * @param request HTTP request containing cookies
     * @return refresh token value or null if not found
     */
    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    /**
     * Sets secure HTTP-only access token cookie.
     * Configures cookie with security attributes: HttpOnly, Secure, SameSite=None,
     * 15-minute expiration, and root path. Critical for protecting access tokens
     * from XSS attacks and ensuring proper token transmission.
     *
     * @param response HTTP response for cookie setting
     * @param token JWT access token value
     */
    private void setAccessToken(HttpServletResponse response, String token) {
        Cookie accessCookie = new Cookie("access_token", token);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 15);
        accessCookie.setAttribute("SameSite", "None");
        response.addCookie(accessCookie);
    }

    /**
     * Sets secure HTTP-only refresh token cookie.
     * Configures cookie with security attributes: HttpOnly, Secure, SameSite=None,
     * 7-day expiration, and restricted path to /jwt/refresh endpoint.
     * Critical for long-term session management and token security.
     *
     * @param response HTTP response for cookie setting
     * @param token JWT refresh token value
     */
    private void setRefreshCookie(HttpServletResponse response, String token) {
        Cookie refreshCookie = new Cookie("refresh_token", token);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/jwt/refresh");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        refreshCookie.setAttribute("SameSite", "None");
        response.addCookie(refreshCookie);
    }

    /**
     * Clears refresh token cookie by setting max age to 0.
     * Removes refresh token from client storage on authentication failures.
     * Uses the same path and security attributes as the original cookie for proper clearing.
     * Critical for security cleanup on token invalidation.
     *
     * @param response HTTP response for cookie clearing
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

}
