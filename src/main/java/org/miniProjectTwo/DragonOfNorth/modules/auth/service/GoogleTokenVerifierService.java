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
     * Validates a Google ID token and returns normalized user claims.
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

    private String maskedClientId() {
        String clientId = config.normalizedClientId();
        if (clientId == null || clientId.isBlank()) {
            return "unset";
        }
        int keep = Math.min(6, clientId.length());
        return clientId.substring(0, keep) + "...";
    }

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
}
