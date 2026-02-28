package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import org.miniProjectTwo.DragonOfNorth.dto.session.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;

import java.util.List;
import java.util.UUID;

public interface SessionService {

    void createSession(AppUser appUser, String rawRefreshToken, String ipAddress, String deviceId, String userAgent);

    void revokeSession(String refreshToken, String deviceId);

    List<SessionSummaryResponse> getSessionsForUser(UUID userId);

    void revokeSessionById(UUID userId, UUID sessionId);

    int revokeAllOtherSessions(UUID userId, String currentDeviceId);

    UUID validateAndRotateSession(String oldRefreshToken, String newRefreshToken, String deviceId);

    void revokeAllSessionsByUserId(UUID userId);
}
