package org.miniProjectTwo.DragonOfNorth.security.service;

import io.jsonwebtoken.Claims;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface JwtServices {
    String generateAccessToken(AuthnFacts authnFacts);

    String generateRefreshToken(UUID userId);

    String buildToken(UUID userId, Map<String, Object> claims, long expiration);

    UUID extractUserId(String token);

    Claims extractAllClaims(String token);

    boolean isTokenExpired(Claims claims);

    void validateTokenType(Claims claims);

    boolean extractMfaVerified(String token);

    Instant extractMfaVerifiedAt(String token);

    List<String> extractAmr(String token);

    UUID extractSessionId(String token);
}
