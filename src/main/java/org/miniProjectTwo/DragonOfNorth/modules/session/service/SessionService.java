package org.miniProjectTwo.DragonOfNorth.modules.session.service;

import org.miniProjectTwo.DragonOfNorth.modules.session.dto.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;

import java.util.List;
import java.util.UUID;

/**
 * Manages user refresh-token sessions across devices.
 */
public interface SessionService {

    /**
     * Creates or replaces the session for the given user/device pair.
     */
    void createSession(AppUser appUser, String rawRefreshToken, String ipAddress, String deviceId, String userAgent);

    /**
     * Revokes the current-device session for the provided refresh token.
     */
    void revokeSession(String refreshToken, String deviceId);

    /**
     * Lists sessions for the given user, newest first.
     */
    List<SessionSummaryResponse> getSessionsForUser(UUID userId);

    /**
     * Revokes one session that belongs to the user.
     */
    void revokeSessionById(UUID userId, UUID sessionId);

    /**
     * Revokes all sessions except the current device.
     */
    int revokeAllOtherSessions(UUID userId, String currentDeviceId);

    /**
     * Validates the old refresh token, rotates session token hash, and returns the user id.
     */
    UUID validateAndRotateSession(String oldRefreshToken, String newRefreshToken, String deviceId);

    /**
     * Revokes all active sessions for a user.
     */
    void revokeAllSessionsByUserId(UUID userId);
}
