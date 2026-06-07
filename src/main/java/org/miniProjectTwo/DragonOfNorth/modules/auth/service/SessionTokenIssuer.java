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

    /**
     * Issues a new login session for the specified user, creating a refresh token and an access token bound to the session. The method generates a refresh token using JWT services, creates a new session record with the provided user and session creation specifications, and then mints an access token that includes the session ID and authentication facts. The resulting access and refresh tokens are returned as an LoginTokens object.
     *
     * @param appUser      The user for whom the login session is being issued.
     * @param creationSpec Specifications for creating the session, including authentication method and other relevant details.
     * @param ipAddress    The IP address from which the login request originated.
     * @param deviceId     An identifier for the device used in the login request.
     * @param userAgent    The user agent string from the login request, providing information about the client's environment.
     * @return A LoginTokens object containing the issued access token and refresh token for the new login session.
     */
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

