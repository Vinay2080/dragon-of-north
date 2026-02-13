package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import jakarta.transaction.Transactional;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.RefreshToken;

public interface RefreshTokenService {
    @Transactional
    void storeRefreshToken(AppUser user, String rawToken);

    @Transactional
    void verifyAndUpdateToken(String rawToken);

    void validateAndUpdateToken(RefreshToken token);

    void deleteRefreshToken(String refreshToken);

}
