package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface OAuthService {
    void authenticatedWithGoogle(String idToken, String deviceId, String expectedIdentifier, HttpServletRequest httpRequest, HttpServletResponse response);

    void signupWithGoogle(String idToken, String deviceId, String expectedIdentifier, HttpServletRequest httpRequest, HttpServletResponse response);
}
