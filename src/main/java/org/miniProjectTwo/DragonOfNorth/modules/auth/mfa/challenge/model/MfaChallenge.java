package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model;

import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.time.Instant;
import java.util.List;

/**
 * Public representation of an MFA challenge.
 *
 * <p>The {@code mfaToken} is opaque (not a JWT) and maps to a mutable Redis
 * {@link ChallengeState}.</p>
 */
public record MfaChallenge(
        String mfaToken,
        Instant expiresAt,
        List<ProviderType> availableMethods
) {
    public MfaChallenge {
        availableMethods = availableMethods == null ? List.of() : List.copyOf(availableMethods);
    }
}

