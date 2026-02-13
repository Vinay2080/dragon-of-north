package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import io.jsonwebtoken.Claims;
import org.miniProjectTwo.DragonOfNorth.model.Role;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface JwtServices {
    String generateAccessToken(UUID userId, Set<Role> roles);

    String generateRefreshToken(UUID userId);

    String buildToken(UUID userId, Map<String, Object> claims, long expiration);

    UUID extractUserId(String token);

    String refreshAccessToken(String refreshToken, Set<Role> roles);

    Claims extractAllClaims(String token);

    boolean isTokenExpired(Claims claims);

    void validateTokenType(Claims claims);
}
