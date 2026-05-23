package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.time.Instant;
import java.util.List;

/**
 * Public response for an MFA challenge that must be completed before issuance.
 */
public record MfaChallengeResponse(
        boolean mfaRequired,
        String challengeId,
        List<ProviderType> availableMethods,
        Instant expiresAt
) {
    public MfaChallengeResponse {
        availableMethods = availableMethods == null ? List.of() : List.copyOf(availableMethods);
    }

    public static MfaChallengeResponse from(MfaChallenge challenge) {
        if (challenge == null) {
            return null;
        }
        return new MfaChallengeResponse(
                true,
                challenge.mfaToken(),
                challenge.availableMethods(),
                challenge.expiresAt()
        );
    }
}
