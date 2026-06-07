package org.miniProjectTwo.DragonOfNorth.security.service;

import io.jsonwebtoken.Claims;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT services interface for generating and validating JWT tokens.
 */
public interface JwtServices {
    /**
     * Generates an access token based on provided authentication facts.
     *
     * @param authnFacts Authentication facts to encode in the token
     * @return Generated access token
     * @throws IllegalArgumentException if authnFacts is null
     */
    String generateAccessToken(AuthnFacts authnFacts);

    /**
     * Generates a refresh token for the given user ID.
     *
     * @param userId User ID for which to generate the refresh token
     * @return Generated refresh token
     * @throws IllegalArgumentException if userId is null
     */
    String generateRefreshToken(UUID userId);

    /**
     * Builds a JWT token with specified user ID, claims, and expiration.
     *
     * @param userId     User ID for which to generate the token
     * @param claims     Additional claims to include in the token
     * @param expiration Token expiration time in milliseconds
     * @return Generated JWT token
     * @throws IllegalArgumentException if userId or claims is null
     */
    String buildToken(UUID userId, Map<String, Object> claims, long expiration);

    /**
     * Extracts the user ID from the provided JWT token.
     *
     * @param token JWT token to extract user ID from
     * @return User ID extracted from the token
     * @throws IllegalArgumentException if the token is null or invalid
     */
    UUID extractUserId(String token);

    /**
     * Extracts all claims from the provided JWT token.
     *
     * @param token JWT token to extract claims from
     * @return Claims extracted from the token
     * @throws IllegalArgumentException if the token is null or invalid
     */
    Claims extractAllClaims(String token);

    /**
     * Checks if the provided JWT claims have expired.
     *
     * @param claims JWT claims to check for expiration
     * @return true if the token is expired, false otherwise
     * @throws IllegalArgumentException if claims are null
     */
    boolean isTokenExpired(Claims claims);

    /**
     * Validates the token type (access or refresh) based on the provided claims.
     *
     * @param claims JWT claims to validate a token type
     * @throws IllegalArgumentException if claims are null or token type is invalid
     */
    void validateTokenType(Claims claims);

    /**
     * Extracts the MFA verified status from the provided JWT token.
     *
     * @param token JWT token to extract MFA verified status from
     * @return true if MFA is verified, false otherwise
     * @throws IllegalArgumentException if the token is null or invalid
     */
    boolean extractMfaVerified(String token);

    /**
     * Extracts the MFA verified timestamp from the provided JWT token.
     *
     * @param token JWT token to extract MFA verified timestamp from
     * @return MFA verified timestamp
     * @throws IllegalArgumentException if the token is null or invalid
     */
    Instant extractMfaVerifiedAt(String token);

    /**
     * Extracts the AMR (Authenticated Method Reference) claims from the provided JWT token.
     *
     * @param token JWT token to extract AMR claims from
     * @return List of AMR claims
     * @throws IllegalArgumentException if the token is null or invalid
     */
    List<String> extractAmr(String token);

    /**
     * Extracts the session ID from the provided JWT token.
     *
     * @param token JWT token to extract session ID from
     * @return Session ID
     * @throws IllegalArgumentException if the token is null or invalid
     */
    UUID extractSessionId(String token);
}
