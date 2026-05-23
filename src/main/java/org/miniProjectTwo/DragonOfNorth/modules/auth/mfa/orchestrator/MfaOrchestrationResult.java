package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.util.List;

/**
 * Result of MFA orchestration before login session issuance.
 */
public record MfaOrchestrationResult(
        boolean mfaRequired,
        List<ProviderType> availableMethods,
        MfaChallenge challenge
) {
    public MfaOrchestrationResult {
        availableMethods = availableMethods == null ? List.of() : List.copyOf(availableMethods);
    }

    public boolean challengeRequired() {
        return challenge != null;
    }

    public static MfaOrchestrationResult noChallenge(boolean mfaRequired, List<ProviderType> availableMethods) {
        return new MfaOrchestrationResult(mfaRequired, availableMethods, null);
    }

    public static MfaOrchestrationResult withChallenge(MfaChallenge challenge) {
        List<ProviderType> methods = challenge == null ? List.of() : challenge.availableMethods();
        return new MfaOrchestrationResult(true, methods, challenge);
    }
}
