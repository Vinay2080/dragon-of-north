package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.util.List;

/**
 * Result of MFA orchestration before login session issuance.
 * <p>Returned by the MFA orchestration service to indicate whether MFA is required, which methods are available, and any active challenge context that the client needs to be aware of. This result informs the client whether it needs to prompt the user for MFA verification and which options to present, as well as providing the necessary context for any ongoing MFA challenge that the client may need to continue with.</p>
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
