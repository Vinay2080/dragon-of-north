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
 * timestamp is older than the policy's max age, callers must complete a step-up MFA
 * challenge before the sensitive operation is allowed to proceed.</p>
 *
 * <p>Design intent: a central policy registry keeps per-endpoint windows consistent
 * while still allowing higher-risk operations to demand stricter freshness.</p>
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

    @NotNull
    private Duration passwordChangeMaxAge = Duration.ofMinutes(5);

    @NotNull
    private Duration accountDeleteMaxAge = Duration.ofSeconds(60);

    @NotNull
    private Duration recoveryCodeRegenerationMaxAge = Duration.ofMinutes(2);

    @NotNull
    private Duration sessionRevokeMaxAge = Duration.ofMinutes(5);

    @NotNull
    private Duration sessionRevokeAllMaxAge = Duration.ofMinutes(5);

    public Duration resolveMaxAge(RecentMfaPolicy policy) {
        if (policy == null) {
            return mfaMaxAge;
        }
        return switch (policy) {
            case PASSWORD_CHANGE -> passwordChangeMaxAge;
            case ACCOUNT_DELETE -> accountDeleteMaxAge;
            case RECOVERY_CODE_REGENERATION -> recoveryCodeRegenerationMaxAge;
            case SESSION_REVOKE -> sessionRevokeMaxAge;
            case SESSION_REVOKE_ALL -> sessionRevokeAllMaxAge;
            case DEFAULT -> mfaMaxAge;
        };
    }
}
