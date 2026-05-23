package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;

/**
 * OAuth authentication entry points used by controllers.
 */
public interface OAuthService {

    /**
     * Authenticates or links a user using a Google ID token.
     */
    void authenticatedWithGoogle(String idToken, String expectedIdentifier, AuthRequestContext context, HttpServletResponse response);

    /**
     * Signs up a new account using a Google ID token.
     */
    void signupWithGoogle(String idToken, String expectedIdentifier, AuthRequestContext context, HttpServletResponse response);
}
