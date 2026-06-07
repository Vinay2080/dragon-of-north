package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;

/**
 * Passwordless login lifecycle contract.
 * <p>
 * Request step issues one-time signed links; verify step validates token freshness, resolves user,
 * and enters the common post-auth issuance pipeline (session cookies or MFA challenge).
 */
public interface PasswordlessLoginService {

    /**
     * Initiates the passwordless login process by generating a one-time token and sending a login link to the specified email address. The token is typically stored in a secure, temporary store (e.g., Redis) with an expiration time. The login link sent to the user includes the token as a query parameter, allowing the user to authenticate by clicking the link.
     *
     * @param email The email address of the user requesting passwordless login. Must not be null or blank.
     */
    void requestPasswordlessLogin(String email);

    /**
     * Verifies the provided passwordless login token, resolves the associated user, and determines the next steps in the authentication process. This method checks the validity and freshness of the token, retrieves the user linked to the token, and then orchestrates the post-authentication flow, which may include issuing session cookies or initiating MFA challenges based on the user's settings and context.
     *
     * @param token    The one-time token received from the passwordless login link. Must not be null or blank.
     * @param context  The authentication request context containing information about the login attempt, such as IP address, user agent, etc.
     * @param response The HTTP response object used to set cookies or headers as needed during the authentication process.
     * @return An MfaOrchestrationResult indicating the outcome of the verification and the next steps in the authentication flow.
     */
    MfaOrchestrationResult verifyPasswordlessLogin(String token, AuthRequestContext context, HttpServletResponse response);
}
