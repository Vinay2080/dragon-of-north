package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import org.miniProjectTwo.DragonOfNorth.dto.auth.response.SessionSummaryResponse;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SessionService {

    @Transactional
    void createSession(AppUser appUser, String rawRefreshToken, String ipAddress, String deviceId, String userAgent);

    @Transactional
    UUID validateAndUpdateSession(String refreshToken, String deviceId);

    @Transactional
    UUID validateAndRotateSession(String oldRefreshToken, String newRefreshToken, String deviceId);

    @Transactional
    void revokeSession(String refreshToken, String deviceId);

    @Transactional(readOnly = true)
    List<SessionSummaryResponse> getSessionsForUser(UUID userId, String currentDeviceId);

    @Transactional
    void revokeSessionById(UUID userId, UUID sessionId);

    @Transactional
    int revokeAllOtherSessions(UUID userId, String currentDeviceId);
}
