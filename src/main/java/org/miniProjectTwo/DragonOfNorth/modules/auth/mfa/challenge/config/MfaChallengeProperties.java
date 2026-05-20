package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration for MFA pre-auth challenges.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "auth.mfa.challenge")
public class MfaChallengeProperties {

    /**
     * How long a challenge state is kept in Redis before expiring.
     */
    @NotNull
    private Duration ttl = Duration.ofMinutes(5);

    /**
     * Maximum allowed verification attempts per challenge.
     */
    @Min(1)
    private int maxAttempts = 5;

    /**
     * How long a user is locked out after exceeding attempt limits.
     */
    @NotNull
    private Duration lockoutTtl = Duration.ofMinutes(15);
}

