package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;

/**
 * OAuth login/sign-up orchestration boundary.
 * <p>
 * Implementations verify third-party identity tokens, reconcile provider linkage with local users,
 * and route successful primary auth through the same session/MFA continuation lifecycle as local auth.
 */
public interface OAuthService {

    /**
     * Authenticates or links a user using a Google ID token.
     */
    MfaOrchestrationResult authenticatedWithGoogle(String idToken, String expectedIdentifier, AuthRequestContext context, HttpServletResponse response);

    /**
     * Signs up a new account using a Google ID token.
     */
    MfaOrchestrationResult signupWithGoogle(String idToken, String expectedIdentifier, AuthRequestContext context, HttpServletResponse response);
}
