package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration for the "recent MFA" step-up authentication window.
 *
 * <p>Controls how long a completed MFA verification is considered "fresh" enough
 * to satisfy sensitive-action guards.  When a session's {@code mfaVerifiedAt}
 * timestamp is older than {@code maxAge}, callers must complete a step-up MFA
 * challenge before the sensitive operation is allowed to proceed.</p>
 *
 * <p>Design intent: a single, centralised value avoids per-endpoint drift and
 * makes security-policy changes a one-line config update.</p>
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.security.step-up")
public class RecentMfaProperties {

    /**
     * Maximum age of a completed MFA verification that satisfies step-up guards.
     *
     * <p>Default is 15 minutes, chosen to balance usability (one typical workflow)
     * with security (short enough to limit the window after a stolen session).</p>
     */
    @NotNull
    private Duration mfaMaxAge = Duration.ofMinutes(15);
}
