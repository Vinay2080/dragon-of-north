package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider;

import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Placeholder verifier for phase 2 challenge lifecycle plumbing.
 */
@Component
public class NoopMfaChallengeProviderVerifier implements MfaChallengeProviderVerifier {
    @Override
    public boolean verify(UUID userId, ProviderType providerType, String code) {
        return false;
    }
}

