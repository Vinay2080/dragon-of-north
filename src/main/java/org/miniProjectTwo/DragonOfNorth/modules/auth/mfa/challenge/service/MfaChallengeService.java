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
 *
 */
public interface MfaChallengeService {

    /**
     * Creates a new MFA challenge for the user and stores its server-side state in Redis.
     * The returned {@code MfaChallenge} contains an opaque token that maps to the Redis state and an expiration time. The available methods are determined based on the user's enrolled MFA methods and the authentication context.
     *
     * @param userId The unique identifier of the user for whom the challenge is being created.
     * @param primaryAmr The primary authentication method reference for the user.
     * @param context The authentication request context, providing additional information about the authentication attempt.
     * @param availableMethods The list of available MFA methods for the user, based on their enrollment and the authentication context.
     * @return A {@code MfaChallenge} object containing the challenge token, expiration time, and available methods.
     * @throws IllegalArgumentException if userId, primaryAmr, or context is null.
     */
    MfaChallenge createChallenge(UUID userId,
                                 String primaryAmr,
                                 AuthRequestContext context,
                                 List<ProviderType> availableMethods);

    /**
     * Creates a step-up MFA challenge bound to the authenticated session.
     *
     * @param userId The unique identifier of the user for whom the step-up challenge is being created.
     * @param sessionId The unique identifier of the authenticated session to which the challenge is bound.
     * @param context The authentication request context, providing additional information about the authentication attempt.
     * @return A {@code MfaChallenge} object containing the challenge token, expiration time, and available methods.
     * @throws IllegalArgumentException if userId, sessionId, or context is null.
     */
    MfaChallenge createStepUpChallenge(UUID userId,
                                       UUID sessionId,
                                       AuthRequestContext context,
                                       List<ProviderType> availableMethods);

    /**
     * Reads the challenge state without mutating it.
     * @param mfaToken The opaque token identifying the MFA challenge.
     * @return The challenge state if found, otherwise an empty Optional.
     * @throws IllegalArgumentException if mfaToken is null or blank.
     */
    Optional<ChallengeState> peek(String mfaToken);

    /**
     * Verifies and atomically consumes a one-time challenge.
     *
     * @param mfaToken The opaque token identifying the MFA challenge.
     * @param providerType The type of MFA provider used for verification.
     * @param code The verification code provided by the user.
     * @param context The authentication request context, providing additional information about the authentication attempt.
     * @return A {@code VerificationResult} indicating the success or failure of the verification attempt.
     * @throws IllegalArgumentException if mfaToken, providerType, code, or context is null.
     */
    VerificationResult verifyAndConsume(String mfaToken,
                                        ProviderType providerType,
                                        String code,
                                        AuthRequestContext context);

    /**
     * Verifies and atomically consumes a one-time challenge bound to a session.
     *
     * @param mfaToken The opaque token identifying the MFA challenge.
     * @param providerType The type of MFA provider used for verification.
     * @param code The verification code provided by the user.
     * @param context The authentication request context, providing additional information about the authentication attempt.
     * @param sessionId The unique identifier of the authenticated session to which the challenge is bound.
     * @return A {@code VerificationResult} indicating the success or failure of the verification attempt.
     * @throws IllegalArgumentException if mfaToken, providerType, code, context, or sessionId is null.
     */
    VerificationResult verifyAndConsume(String mfaToken,
                                        ProviderType providerType,
                                        String code,
                                        AuthRequestContext context,
                                        UUID sessionId);

    /**
     * Invalidates the challenge token and deletes its Redis state.
     * @apiNote This should be called after successful verification to prevent reuse or after failed attempts to enforce lockout policies. It can also be used to manually expire a challenge if needed (e.g., a user cancels the login process).
     * @param mfaToken The opaque token identifying the MFA challenge to invalidate.
     * @param context The authentication request context, providing additional information about the authentication attempt.
     * @throws IllegalArgumentException if mfaToken, sessionId, or context is null.
     */
    void invalidate(String mfaToken, AuthRequestContext context);
}
