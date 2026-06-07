package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model;

import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.time.Instant;
import java.util.List;

/**
 * Public representation of an MFA challenge.
 * <p>Returned by {@code /auth/mfa/challenge} to signal the need for MFA verification and provide necessary context for the client to proceed. The {@code mfaToken} is a unique identifier for the MFA challenge, used in subsequent verification requests to correlate with the pending MFA context stored in Redis. The {@code expiresAt} timestamp indicates when the challenge will expire and no longer be valid for verification. The {@code availableMethods} list informs the client of which MFA providers are available for this challenge, allowing it to present appropriate options to the user.</p>
 * <p>Security expectations: the {@code mfaToken} should be a securely generated opaque value that cannot be easily guessed or forged, and should be tied to a specific user session and MFA context in Redis. The expiration time should be reasonably short (e.g., 5-10 minutes) to limit the window of opportunity for abuse. The available methods should be determined based on the user's enrolled MFA methods and the context of the authentication attempt (e.g., device trust level, location, etc.) to provide a balance between security and usability.</p>
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

