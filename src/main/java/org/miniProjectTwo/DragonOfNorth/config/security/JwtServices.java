package org.miniProjectTwo.DragonOfNorth.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

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

    /**
     * Username validation pattern: 3–30 characters, alphanumeric, or underscore.
     */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

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
        this.privateKey = KeyUtils.loadPrivateKey("local-keys/private_key.pem");
        this.publicKey = KeyUtils.loadPublicKey("local-keys/public_key.pem");
        log.info("JWT RSA keys successfully loaded");
    }

    /**
     * Generates a signed JWT access token for the given username.
     *
     * @param username an authenticated username
     * @return a compact JWT access token string
     */
    public String generateAccessToken(final String username) {
        validateUsername(username);

        Map<String, Object> claims = Map.of(TOKEN_TYPE, ACCESS_TOKEN_TYPE);
        return buildToken(username, claims, accessTokenExpiration);
    }

    /**
     * Generates a signed JWT refresh token for the given username.
     *
     * @param username an authenticated username
     * @return a compact JWT refresh token string
     */
    public String generateRefreshToken(final String username) {
        validateUsername(username);

        Map<String, Object> claims = Map.of(TOKEN_TYPE, REFRESH_TOKEN_TYPE);
        return buildToken(username, claims, refreshTokenExpiration);
    }

    /**
     * Builds a signed JWT using an RSA private key, embedding claims, subject,
     * issue time, and expiration time.
     *
     * @param username   the subject (typically the username)
     * @param claims     token claims to embed
     * @param expiration validity duration in milliseconds
     * @return a signed JWT string
     */
    private String buildToken(String username, Map<String, Object> claims, long expiration) {
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(claims, "claims cannot be null");

        final Date issuedAt = new Date();
        final Date expiry = new Date(issuedAt.getTime() + expiration);

        log.debug("Generating {} for username={}", claims.get(TOKEN_TYPE), username);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .signWith(privateKey)
                .issuedAt(issuedAt)
                .expiration(expiry)
                .compact();
    }


    /**
     * Generates a new access token using a valid refresh token.
     *
     * @param refreshToken the provided refresh token
     * @return a new access token
     */
    public String refreshAccessToken(final String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token cannot be empty");
        }

        Claims claims = extractAllClaims(refreshToken);

        validateTokenType(claims);

        if (isTokenExpired(claims)) {
            log.warn("Refresh token expired for user={}", claims.getSubject());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Refresh token has expired");
        }

        return generateAccessToken(claims.getSubject());
    }

    /**
     * Extracts all claims from the given JWT. Performs signature verification and
     * maps parsing errors to {@link BusinessException} with appropriate error codes.
     *
     * @param token the JWT string
     * @return extracted {@link Claims}
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
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
     * Extracts the username (JWT subject) from a token.
     *
     * @param token the JWT string
     * @return the username contained within the token
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }


    /**
     * Validates a JWT by ensuring:
     * <ul>
     *     <li>The subject matches the user's username</li>
     *     <li>The token has not expired</li>
     * </ul>
     *
     * @param token       the JWT string
     * @param userDetails Spring Security user details
     * @return true if the token is valid and belongs to the user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);

        String username = claims.getSubject();
        boolean usernameMatch = username.equals(userDetails.getUsername());
        boolean expired = isTokenExpired(claims);

        log.debug("Token validation: usernameMatch={}, expired={}", usernameMatch, expired);

        return usernameMatch && !expired;
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
     * Validates a username format using a predefined regex.
     *
     * @param username the username to validate
     * @throws IllegalArgumentException if blank or invalid format
     */
    private void validateUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Invalid username format");
        }
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
