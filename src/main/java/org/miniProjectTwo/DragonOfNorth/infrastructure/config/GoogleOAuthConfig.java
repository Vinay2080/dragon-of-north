package org.miniProjectTwo.DragonOfNorth.infrastructure.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;


/**
 * Binds Google OAuth settings and exposes Google token verification beans.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "google")
@Validated
public class GoogleOAuthConfig {

    @NotBlank(message = "Google client Id is required")
    private String clientId;

    /**
     * Creates a verifier used to validate Google ID tokens.
     *
     * @return Google ID token verifier
     */
    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
        ).build();
    }

    /**
     * Returns the configured client id with surrounding whitespace removed.
     *
     * @return trimmed client id, or {@code null} when not configured
     */
    public String normalizedClientId() {
        return clientId == null ? null : clientId.trim();
    }

}
