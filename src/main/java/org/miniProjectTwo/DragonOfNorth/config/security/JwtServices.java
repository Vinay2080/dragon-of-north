package org.miniProjectTwo.DragonOfNorth.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.exception.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
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
 *     <li>Refreshing expired access tokens using valid refresh tokens</li>
 * </ul>
 *
 * <p>Tokens are signed with the RSA private key and verified using the RSA public key.
 * Token expiration durations are configured through application properties.</p>
 */
@Slf4j
@Service
public class JwtServices {

    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access_token";
    private static final String REFRESH_TOKEN_TYPE = "refresh_token";
    private static final String ISSUER = "dragon-of-north-auth";
    private static final String ROLES = "roles";


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
    public JwtServices() throws Exception {
        this.privateKey = KeyUtils.loadPrivateKey("keys/private_key.pem");
        this.publicKey = KeyUtils.loadPublicKey("keys/public_key.pem");
        log.info("JWT RSA keys successfully loaded");
    }

    /**
     * Generates a signed JWT access token for the given username.
     *
     * @return a compact JWT access token string
     */
    public String generateAccessToken(UUID userId, Set<Role> roles) {

        List<String> roleNames = roles
                .stream()
                .map(role -> role.getRoleName().name())
                .toList();

        Map<String, Object> claims = Map.of(TOKEN_TYPE, ACCESS_TOKEN_TYPE, ROLES, roleNames);

        return buildToken(userId, claims, accessTokenExpiration);
    }

    /**
     * Generates a signed JWT refresh token for the given username.
     *
     * @return a compact JWT refresh token string
     */
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
    private String buildToken(UUID userId, Map<String, Object> claims, long expiration) {
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


    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    /**
     * Generates a new access token using a valid refresh token.
     *
     * @param refreshToken the provided refresh token
     * @return a new access token
     */
    public String refreshAccessToken(final String refreshToken, Set<Role> roles) {
        if (StringUtils.isBlank(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token cannot be empty");
        }

        Claims claims = extractAllClaims(refreshToken);

        validateTokenType(claims);

        if (isTokenExpired(claims)) {
            log.warn("Refresh token expired for user={}", claims.getSubject());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token has expired");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        return generateAccessToken(userId, roles);
    }

    /**
     * Extracts all claims from the given JWT. Performs signature verification and
     * maps parsing errors to {@link BusinessException} with appropriate error codes.
     *
     * @param token the JWT string
     * @return extracted {@link Claims}
     */
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
    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }


    /**
     * Ensures the provided token contains a refresh-token type.
     *
     * @param claims the token claims extracted from the JWT
     * @throws BusinessException if the token is not a refresh token
     */
    private void validateTokenType(Claims claims) {
        String tokenType = claims.get(TOKEN_TYPE, String.class);

        if (!REFRESH_TOKEN_TYPE.equals(tokenType)) {
            log.warn("Invalid token type: expected={}, actual={}", REFRESH_TOKEN_TYPE, tokenType);
            throw new BusinessException(
                    ErrorCode.INVALID_TOKEN,
                    String.format("Invalid token type: expected %s but received %s", REFRESH_TOKEN_TYPE, tokenType)
            );
        }
    }
}
// todo javadoc