package org.miniProjectTwo.DragonOfNorth.security.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.security.service.AuthnFacts;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.security.util.KeyUtils;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;

/**
 * Service responsible for creating, validating, and parsing JSON Web Tokens (JWT)
 * signed with RSA asymmetric keys.
 *
 * <p>This service supports:
 * <ul>
 *     <li>Generating access and refresh tokens</li>
 *     <li>Extracting claims and subjects from tokens</li>
 *     <li>Validating token integrity and expiration</li>
 * </ul>
 *
 * <p>Tokens are signed with the RSA private key and verified using the RSA public key.
 * Token expiration durations are configured through application properties.</p>
 */
@Slf4j
@Service
/**
 * Default JWT implementation for access/refresh issuance and validation.
 * <p>
 * Encodes authentication facts used by filters and session enforcement. Any claim-format change
 * must be coordinated with JwtFilter, SidLivenessFilter, and session token issuance components.
 */
public class JwtServicesImpl implements JwtServices {

    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access_token";
    private static final String REFRESH_TOKEN_TYPE = "refresh_token";
    private static final String ISSUER = "dragon-of-north-auth";
    private static final String ROLES = "roles";
    private static final String MFA_VERIFIED = "mfa_verified";
    private static final String MFA_VERIFIED_AT = "mfa_verified_at";
    private static final String AMR = "amr";
    private static final String SESSION_ID = "sid";


    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    @Value("${app.security.jwt.expiration.access-token}")
    private long accessTokenExpiration;

    @Value("${app.security.jwt.expiration.refresh-token}")
    private long refreshTokenExpiration;

    /**
     * Loads RSA keys from the application's classpath.
     *
     * @throws Exception if key loading fails
     */
    public JwtServicesImpl(
            @Value("${keys.private}") String privateKeyPath,
            @Value("${keys.public}") String publicKeysPath
    ) throws Exception {
        this.privateKey = KeyUtils.loadPrivateKey(privateKeyPath);
        this.publicKey = KeyUtils.loadPublicKey(publicKeysPath);
        log.info("JWT RSA keys successfully loaded");
    }

    @Override
    public String generateAccessToken(AuthnFacts authnFacts) {
        Objects.requireNonNull(authnFacts, "authnFacts cannot be null");

        List<String> roles = authnFacts.roles() == null ? List.of() : List.copyOf(authnFacts.roles());
        List<String> amr = authnFacts.amr() == null ? List.of() : List.copyOf(authnFacts.amr());

        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE, ACCESS_TOKEN_TYPE);
        claims.put(ROLES, roles);
        claims.put(MFA_VERIFIED, authnFacts.mfaVerified());
        claims.put(AMR, amr);

        if (authnFacts.mfaVerifiedAt() != null) {
            claims.put(MFA_VERIFIED_AT, Date.from(authnFacts.mfaVerifiedAt()));
        }

        if (authnFacts.sessionId() != null) {
            claims.put(SESSION_ID, authnFacts.sessionId().toString());
        }

        return buildToken(authnFacts.userId(), claims, accessTokenExpiration);
    }

    /**
     * Generates a signed JWT refresh token for the given username.
     *
     * @return a compact JWT refresh token string
     */
    @Override
    public String generateRefreshToken(UUID userId) {

        Map<String, Object> claims = Map.of(TOKEN_TYPE, REFRESH_TOKEN_TYPE);
        return buildToken(userId, claims, refreshTokenExpiration);
    }

    /**
     * Builds a signed JWT using an RSA private key, embedding claims, subject,
     * issue time, and expiration time.
     *
     * @param claims     token claims to embed
     * @param expiration validity duration in milliseconds
     * @return a signed JWT string
     */
    @Override
    public String buildToken(UUID userId, Map<String, Object> claims, long expiration) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(claims, "claims cannot be null");

        final Date issuedAt = new Date();
        final Date expiry = new Date(issuedAt.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .issuer(ISSUER)
                .subject(userId.toString())
                .signWith(privateKey)
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiration(expiry)
                .compact();
    }


    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }


    /**
     * Extracts all claims from the given JWT. Performs signature verification and
     * maps parsing errors to {@link BusinessException} with appropriate error codes.
     *
     * @param token the JWT string
     * @return extracted {@link Claims}
     */
    @Override
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (io.jsonwebtoken.security.SignatureException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid JWT signature");

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "JWT token is expired");

        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_TOKEN);

        } catch (io.jsonwebtoken.MalformedJwtException e) {
            throw new BusinessException(ErrorCode.MALFORMED_TOKEN);

        } catch (io.jsonwebtoken.JwtException e) {
            throw new BusinessException(ErrorCode.ILLEGAL_TOKEN, e.getMessage());
        }
    }


    /**
     * Checks if a token represented by claims is expired.
     *
     * @param claims token claims
     * @return true if expired
     */
    @Override
    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }


    /**
     * Ensures the provided token contains a refresh-token type.
     *
     * @param claims the token claims extracted from the JWT
     * @throws BusinessException if the token is not a refresh token
     */
    @Override
    public void validateTokenType(Claims claims) {
        String tokenType = claims.get(TOKEN_TYPE, String.class);

        if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
            log.warn("Invalid token type: expected={}, actual={}", REFRESH_TOKEN_TYPE, tokenType);
            throw new BusinessException(
                    ErrorCode.INVALID_TOKEN,
                    String.format("Invalid token type: expected %s but received %s", REFRESH_TOKEN_TYPE, tokenType)
            );
        }
    }

    @Override
    public boolean extractMfaVerified(String token) {
        Claims claims = extractAllClaims(token);
        Boolean value = claims.get(MFA_VERIFIED, Boolean.class);
        return Boolean.TRUE.equals(value);
    }

    @Override
    public Instant extractMfaVerifiedAt(String token) {
        Claims claims = extractAllClaims(token);
        Object value = claims.get(MFA_VERIFIED_AT);
        return toInstant(value);
    }

    @Override
    public List<String> extractAmr(String token) {
        Claims claims = extractAllClaims(token);
        Object value = claims.get(AMR);
        if (value instanceof List<?> list) {
            List<String> amr = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    amr.add(entry.toString());
                }
            }
            return List.copyOf(amr);
        }
        return List.of();
    }

    @Override
    public UUID extractSessionId(String token) {
        Claims claims = extractAllClaims(token);
        String raw = claims.get(SESSION_ID, String.class);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Instant toInstant(Object value) {
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return null;
    }
}

//todo add audience intended for in JWT.