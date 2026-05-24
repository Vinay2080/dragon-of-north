package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.SessionCreationSpec;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.security.service.SessionAccessTokenIssuer;
import org.springframework.stereotype.Service;

/**
 * Issues refresh/access token pairs backed by persisted session records.
 * <p>
 * Dependency chain: generate refresh token -> persist session metadata -> mint access token bound
 * to session id/authentication facts. This keeps token issuance uniform for local, OAuth, and
 * passwordless entry points and enables centralized future evolution (device trust, step-up auth,
 * distributed token minting).
 */
@Service
@RequiredArgsConstructor
public class SessionTokenIssuer {

    private final JwtServices jwtServices;
    private final SessionService sessionService;
    private final SessionAccessTokenIssuer sessionAccessTokenIssuer;

    public LoginTokens issueLoginSession(AppUser appUser,
                                         String primaryAmr,
                                         String ipAddress,
                                         String deviceId,
                                         String userAgent) {
        return issueLoginSession(appUser, SessionCreationSpec.fromAppUser(appUser, primaryAmr), ipAddress, deviceId, userAgent);
    }

    public LoginTokens issueLoginSession(AppUser appUser,
                                         SessionCreationSpec creationSpec,
                                         String ipAddress,
                                         String deviceId,
                                         String userAgent) {
        String refreshToken = jwtServices.generateRefreshToken(appUser.getId());
        Session session = sessionService.createSession(
                appUser,
                refreshToken,
                ipAddress,
                deviceId,
                userAgent,
                creationSpec
        );
        String accessToken = sessionAccessTokenIssuer.mintAccessToken(session, appUser.getRoles());
        return new LoginTokens(accessToken, refreshToken);
    }

    public record LoginTokens(String accessToken, String refreshToken) {
    }
}

