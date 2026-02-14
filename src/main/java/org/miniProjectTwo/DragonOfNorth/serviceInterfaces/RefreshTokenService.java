package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import jakarta.transaction.Transactional;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.RefreshToken;

import java.util.List;

public interface RefreshTokenService {
    @Transactional
    void storeRefreshToken(AppUser user, String rawToken);

    @Transactional
    void verifyAndUpdateToken(String rawToken);

    void validateAndUpdateToken(RefreshToken token);

    @Transactional
    void revokeToken(RefreshToken token);

    @Transactional
    void revokeTokenByRawToken(String rawToken);

    List<RefreshToken> findValidTokensByUser(AppUser appUser);

    @Transactional
    void deleteAllTokensForUser(AppUser appUser);
}
