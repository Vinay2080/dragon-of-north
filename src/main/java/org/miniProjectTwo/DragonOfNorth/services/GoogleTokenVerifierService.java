package org.miniProjectTwo.DragonOfNorth.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.config.GoogleOAuthConfig;
import org.miniProjectTwo.DragonOfNorth.dto.OAuth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.exception.InvalidOAuthTokenException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;


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

    public OAuthUserInfo verifyToken(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                log.warn("Google ID token verification failed: null token");
                throw new InvalidOAuthTokenException();
            }
            GoogleIdToken.Payload payload = token.getPayload();

            String issuer = payload.getIssuer();
            if (!ALLOWED_ISSUER.contains(issuer)) {
                log.warn("Invalid issuer: {}", issuer);
                throw new InvalidOAuthTokenException("invalid token");
            }
            // Explicit audience validation (Google may return either a single string or a list)
            String audience = resolveAudience(payload.get("aud"));
            if (audience == null || !config.getClientId().equals(audience)) {
                log.warn("Invalid audience: expected={}, actual={}", config.getClientId(), payload.get("aud"));
                throw new InvalidOAuthTokenException("Invalid token");
            }

            // Enforce email verification
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                log.warn("Email not verified for user: {}", payload.getEmail());
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
        if (audienceClaim instanceof String audience) {
            return audience;
        }

        if (audienceClaim instanceof Collection<?> audiences) {
            return audiences.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(config.getClientId()::equals)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }
}
