package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface SessionService {

    @Transactional
    void createSession(AppUser appUser, String rawRefreshToken, String ipAddress, String deviceId, String userAgent);

    @Transactional
    UUID validateAndUpdateSession(String refreshToken, String deviceId);

    @Transactional
    void revokeSession(String refreshToken, String deviceId);
}
