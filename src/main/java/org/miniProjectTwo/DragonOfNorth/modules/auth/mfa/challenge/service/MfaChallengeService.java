package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service;

import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.ChallengeState;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.VerificationResult;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Challenge lifecycle contract for creating, loading, and completing MFA challenges.
 */
public interface MfaChallengeService {

    /**
     * Creates a new MFA challenge for the user and stores its server-side state in Redis.
     */
    MfaChallenge createChallenge(UUID userId,
                                 String primaryAmr,
                                 AuthRequestContext context,
                                 List<ProviderType> availableMethods);

    /**
     * Reads the challenge state without mutating it.
     */
    Optional<ChallengeState> peek(String mfaToken);

    /**
     * Verifies and atomically consumes a one-time challenge.
     */
    VerificationResult verifyAndConsume(String mfaToken,
                                        ProviderType providerType,
                                        String code,
                                        AuthRequestContext context);

    /**
     * Invalidates the challenge token and deletes its Redis state.
     */
    void invalidate(String mfaToken, AuthRequestContext context);
}
