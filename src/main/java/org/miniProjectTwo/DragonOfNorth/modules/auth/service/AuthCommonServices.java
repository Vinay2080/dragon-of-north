package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.SessionCreationSpec;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;

/**
 * Shared authentication operations used across identifier strategies.
 */
public interface AuthCommonServices {

    /**
     * Authenticates user credentials and issues auth cookies/session.
     */
    void login(String identifier, String password, HttpServletResponse response, AuthRequestContext context);

    /**
     * Rotates refresh/access tokens for the active device.
     */
    void refreshToken(String refreshToken, HttpServletResponse response, AuthRequestContext context);

    /**
     * Ensures the user has the default role if no roles are assigned.
     */
    void assignDefaultRole(AppUser appUser);

    /**
     * Updates account status.
     */
    void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser);

    /**
     * Revokes current-device session and clears auth cookies.
     */
    void logoutUser(String refreshToken, HttpServletResponse response, AuthRequestContext context);

    /**
     * Soft deletes the authenticated account, revokes sessions, and clears auth cookies.
     */
    void deleteAccount(HttpServletResponse response, AuthRequestContext context);

    /**
     * Returns the currently authenticated user.
     */
    AppUser findAuthenticatedUser();

    /**
     * Ensures identifier verification and completes session+cookie login issuance.
     */
    void completeLogin(AppUser appUser, String identifier, HttpServletResponse response, AuthRequestContext context);

    /**
     * Writes the access-token cookie.
     */
    void setAccessToken(HttpServletResponse response, String token);

    /**
     * Writes the refresh-token cookie.
     */
    void setRefreshToken(HttpServletResponse response, String token);

    /**
     * Central login issuance entry point for auth flows (session + JWT + cookies).
     */
    void issueLoginSession(AppUser appUser, SessionCreationSpec creationSpec, HttpServletResponse response, AuthRequestContext context);
}
