package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider;

import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.util.UUID;

public interface MfaChallengeProviderVerifier {
    ChallengeProviderVerification verify(UUID userId, ProviderType providerType, String code);
}
