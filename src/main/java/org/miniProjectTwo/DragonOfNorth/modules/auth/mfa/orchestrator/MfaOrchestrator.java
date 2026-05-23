package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.MfaChallengeService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider.MfaProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.registry.MfaProviderRegistry;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Centralized MFA decision point for login-like flows.
 */
@Service
@RequiredArgsConstructor
public class MfaOrchestrator {

    private final MfaChallengeService challengeService;
    private final MfaProviderRegistry providerRegistry;

    public MfaOrchestrationResult orchestrateLogin(AppUser user, String primaryAmr, AuthRequestContext context) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (primaryAmr == null || primaryAmr.isBlank()) {
            throw new IllegalArgumentException("primaryAmr must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        List<ProviderType> availableMethods = providerRegistry.getAvailableProviders(user).stream()
                .filter(MfaProvider::allowsLoginChallenge)
                .map(MfaProvider::type)
                .toList();

        boolean mfaRequired = user.isMfaEnabled() && !availableMethods.isEmpty();
        if (!mfaRequired) {
            return MfaOrchestrationResult.noChallenge(false, availableMethods);
        }

        MfaChallenge challenge = challengeService.createChallenge(user.getId(), primaryAmr, context, availableMethods);
        return MfaOrchestrationResult.withChallenge(challenge);
    }
}
