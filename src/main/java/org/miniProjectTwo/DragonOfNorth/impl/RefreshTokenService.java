package org.miniProjectTwo.DragonOfNorth.impl;

import jakarta.transaction.Transactional;
import org.miniProjectTwo.DragonOfNorth.components.TokenHasher;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.RefreshToken;
import org.miniProjectTwo.DragonOfNorth.repositories.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for managing JWT refresh tokens with secure storage and validation.
 * <p>
 * Handles token creation, verification, and expiration management.
 * Uses secure hashing and database persistence for session renewal.
 * Critical for maintaining secure user authentication flows.
 *
 * @see TokenHasher for secure token operations
 * @see RefreshToken for token entity
 */
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

    /**
     * Stores refresh token with secure hashing and expiration.
     * <p>
     * Hashes token, links to user, sets prefix, and calculates expiration.
     * Persists in a database for future authentication renewal.
     * Critical for secure session management.
     *
     * @param user     token owner
     * @param rawToken unhashed refresh token
     */
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

    /**
     * Verifies the refresh token and updates the last used timestamp.
     * <p>
     * Validates token prefix, hash, expiration, and revocation status.
     * Updates last used time on successful verification.
     * Critical for token security and session continuity.
     *
     * @param rawToken refresh token to verify
     * @throws BusinessException if token is invalid, expired, or revoked
     */
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

    /**
     * Validates token status and updates usage timestamp.
     * <p>
     * Checks expiration and revocation before allowing usage.
     * Updates last used timestamp for security tracking.
     * Critical for preventing token reuse and ensuring validity.
     *
     * @param token refresh token to validate
     * @throws BusinessException if the token is expired or revoked
     */
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
