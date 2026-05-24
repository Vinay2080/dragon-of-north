package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.VerificationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.SessionCreationSpec;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;

/**
 * Shared orchestration contract for local, OAuth, and passwordless authentication flows.
 * <p>
 * This boundary centralizes lifecycle steps that must remain consistent regardless of entry path:
 * credential verification, user-state validation, session creation, JWT cookie issuance, refresh,
 * logout, and account deletion side effects.
 */
public interface AuthCommonServices {

    /**
     * Authenticates user credentials and issues auth cookies/session.
     */
    MfaOrchestrationResult login(String identifier, String password, HttpServletResponse response, AuthRequestContext context);

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
    MfaOrchestrationResult completeLogin(AppUser appUser, String identifier, HttpServletResponse response, AuthRequestContext context);

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
    MfaOrchestrationResult issueLoginSession(AppUser appUser, SessionCreationSpec creationSpec, HttpServletResponse response, AuthRequestContext context);

    /**
     * Completes authentication after MFA challenge verification and challenge consumption.
     */
    VerificationResult completeMfaChallengeLogin(String challengeId, String code, org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType providerType, HttpServletResponse response, AuthRequestContext context);
}
