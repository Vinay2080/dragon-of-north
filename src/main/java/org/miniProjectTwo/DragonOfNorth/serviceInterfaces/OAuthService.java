package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface OAuthService {
    void authenticatedWithGoogle(String idToken, String deviceId, HttpServletRequest httpRequest, HttpServletResponse response);
}
