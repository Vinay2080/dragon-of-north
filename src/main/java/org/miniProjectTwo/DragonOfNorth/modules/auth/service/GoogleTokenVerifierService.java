package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.infrastructure.config.GoogleOAuthConfig;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.exception.InvalidOAuthTokenException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * Verifies Google ID tokens and maps verified claims into {@link OAuthUserInfo}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenVerifierService {

    private final static Set<String> ALLOWED_ISSUER = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );
    private final GoogleIdTokenVerifier verifier;
    private final GoogleOAuthConfig config;
    private final ObjectMapper objectMapper;


    /**
     * Verifies the provided Google ID token and extracts user information if valid. This method uses the GoogleIdTokenVerifier to validate the token's signature and claims. It checks the issuer against allowed Google issuers, validates the audience to ensure it matches the expected client ID, and enforces that the email is verified. If the token is valid, it maps relevant claims into an OAuthUserInfo object. If any validation step fails, it logs detailed diagnostics and throws an InvalidOAuthTokenException.
     *
     * @param idToken The Google ID token to verify.
     * @return An OAuthUserInfo object containing user details extracted from the token if verification is successful.
     * @throws InvalidOAuthTokenException If the token is invalid, expired, or fails any of the verification checks.
     */
    public OAuthUserInfo verifyToken(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                Map<String, Object> payload = decodeUnverifiedPayload(idToken);
                log.warn("Google ID token verification failed: null token. diagnostics: expectedClientId={}, tokenLength={}, hasAudienceClaim={}, hasIssuerClaim={}",
                        maskedClientId(), idToken == null ? 0 : idToken.length(), payload.containsKey("aud"), payload.containsKey("iss"));
                throw new InvalidOAuthTokenException();
            }
            GoogleIdToken.Payload payload = token.getPayload();

            String issuer = payload.getIssuer();
            if (!ALLOWED_ISSUER.contains(issuer)) {
                log.warn("Google ID token rejected due to issuer validation");
                throw new InvalidOAuthTokenException("invalid token");
            }
            // Explicit audience validation (Google may return either a single string or a list)
            String audience = resolveAudience(payload.get("aud"));
            String expectedClientId = config.normalizedClientId();
            if (expectedClientId == null || !expectedClientId.equals(audience)) {
                log.warn("Google ID token rejected due to audience validation. expectedClientId={}", maskedClientId());
                throw new InvalidOAuthTokenException("Invalid token");
            }

            // Enforce email verification
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                log.warn("Google ID token rejected because email is not verified");
                throw new InvalidOAuthTokenException("Email not verified");
            }


            return OAuthUserInfo.builder()
                    .sub(payload.getSubject())
                    .email(payload.getEmail())
                    .emailVerified(Boolean.TRUE.equals(payload.getEmailVerified()))
                    .name((String) payload.get("name"))
                    .picture((String) payload.get("picture"))
                    .issuer(payload.getIssuer())
                    .audience(audience)
                    .expirationTime(payload.getExpirationTimeSeconds())
                    .issuedAtTime(payload.getIssuedAtTimeSeconds())
                    .build();
        } catch (InvalidOAuthTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification failed", e);
            throw new InvalidOAuthTokenException();
        }
    }

    /**
     * Decodes the unverified payload of a Google ID token.
     *
     * @param idToken The Google ID token to decode.
     * @return A map containing the decoded payload claims.
     */
    private Map<String, Object> decodeUnverifiedPayload(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            return Map.of();
        }

        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return Map.of();
            }

            String decodedPayload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return objectMapper.readValue(decodedPayload, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /**
     * Returns a masked version of the client ID for logging purposes.
     *
     * @return The masked client ID.
     */
    private String maskedClientId() {
        String clientId = config.normalizedClientId();
        if (clientId == null || clientId.isBlank()) {
            return "unset";
        }
        int keep = Math.min(6, clientId.length());
        return clientId.substring(0, keep) + "...";
    }

    /**
     * Resolves the audience claim from the token payload, handling both string and array formats. Google ID tokens may return the "aud" claim as either a single string or an array of strings. This method checks the type of the claim and extracts the audience accordingly. If the claim is a string, it trims whitespace and returns it. If it's a collection, it filters for string entries, trims them, and checks if any match the expected client ID from the configuration. If a match is found, it returns that audience; otherwise, it returns null.
     *
     * @param audienceClaim The raw audience claim extracted from the token payload, which may be a String or a Collection.
     * @return The resolved audience string if valid, or null if it cannot be resolved or does not match the expected client ID.
     */
    private String resolveAudience(Object audienceClaim) {
        String expectedClientId = config.normalizedClientId();
        if (audienceClaim instanceof String audience) {
            return audience.trim();
        }

        if (audienceClaim instanceof Collection<?> audiences) {
            return audiences.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::trim)
                    .filter(expectedClientId::equals)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }
}
