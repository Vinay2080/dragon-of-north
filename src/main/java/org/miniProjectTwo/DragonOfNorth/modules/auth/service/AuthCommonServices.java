package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.VerificationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.SessionCreationSpec;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.util.UUID;

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
    VerificationResult completeMfaChallengeLogin(String challengeId, String code, ProviderType providerType, HttpServletResponse response, AuthRequestContext context);

    /**
     * Issues a step-up MFA challenge for an already-authenticated user.
     *
     * <p>Uses the same challenge infrastructure as login-time MFA so there is a single
     * challenge lifecycle — no second MFA system is introduced.</p>
     *
     * @param user      the currently-authenticated user
     * @param sessionId authenticated session id for binding
     * @param context   request metadata for binding and audit
     * @return the MFA challenge token and expiry for the client to present to the verify endpoint
     */
    MfaChallenge issueStepUpChallenge(AppUser user, UUID sessionId, AuthRequestContext context);

    /**
     * Verifies a step-up MFA challenge, then updates the session's {@code mfa_verified_at}
     * timestamp and re-mints the access token so all downstream claims reflect the refresh.
     *
     * <p>Session truth is updated centrally here — no caller needs to patch the JWT or
     * the session themselves.</p>
     *
     * @param challengeId  the opaque step-up challenge token
     * @param providerType the MFA provider used (TOTP, RECOVERY_CODE, …)
     * @param code         the submitted verification code
     * @param sessionId    ID of the authenticated session to refresh (from the JWT {@code sid} claim)
     * @param response     used to write the refreshed access-token cookie
     * @param context      request metadata for binding and audit
     */
    void completeStepUpMfaChallenge(String challengeId,
                                    ProviderType providerType,
                                    String code,
                                    UUID sessionId,
                                    HttpServletResponse response,
                                    AuthRequestContext context);
}
