package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * OAuth authentication entry points used by controllers.
 */
public interface OAuthService {

    /**
     * Authenticates or links a user using a Google ID token.
     */
    void authenticatedWithGoogle(String idToken, String deviceId, String expectedIdentifier, HttpServletRequest httpRequest, HttpServletResponse response);

    /**
     * Signs up a new account using a Google ID token.
     */
    void signupWithGoogle(String idToken, String deviceId, String expectedIdentifier, HttpServletRequest httpRequest, HttpServletResponse response);
}
