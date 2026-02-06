package org.miniProjectTwo.DragonOfNorth.services;

import jakarta.transaction.Transactional;
import org.miniProjectTwo.DragonOfNorth.components.TokenHasher;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.exception.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.RefreshToken;
import org.miniProjectTwo.DragonOfNorth.repositories.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RefreshTokenService {

    private final TokenHasher tokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    @Value("${app.security.jwt.expiration.refresh-token}")
    private long refreshTokenDurationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, TokenHasher tokenHasher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public void storeRefreshToken(AppUser user, String rawToken) {
        String token = tokenHasher.hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(token);
        refreshToken.setTokenPrefix(token.substring(0, 8));
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void verifyAndUpdateToken(String rawToken) {
        String prefix = rawToken.length() > 8 ? rawToken.substring(0, 8) : rawToken;

        refreshTokenRepository.findByTokenPrefix(prefix)
                .stream()
                .filter(rt -> tokenHasher.matches(rawToken, rt.getToken()))
                .findFirst()
                .ifPresentOrElse(
                        this::validateAndUpdateToken,
                        () -> {
                            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token");
                        }
                );
    }

    private void validateAndUpdateToken(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token expired");
        }

        if (token.isRevoked()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token revoked");
        }

        token.setLastUsed(Instant.now());
        refreshTokenRepository.save(token);
    }

}
