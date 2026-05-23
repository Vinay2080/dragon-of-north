package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mutable server-side state for an in-progress MFA challenge.
 *
 * <p>Stored in Redis and addressed by an opaque token. This state exists after
 * primary authentication but before a full authenticated session is created.</p>
 */
public record ChallengeState(
        UUID userId,
        String primaryAmr,
        String deviceId,
        String ipPrefix,
        String userAgentHash,
        List<org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType> allowedProviders,
        int attempts,
        Instant createdAt,
        Instant expiresAt
) {
}
