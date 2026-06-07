package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.provider;

/**
 * Result of an MFA challenge verification attempt.
 * <p>
 * Encapsulates the success status and failure reason (if applicable) of MFA challenge verification. Used by MFA providers to communicate the outcome of a verification attempt back to the MFA service, which can then determine whether to proceed with authentication or reject the attempt based on the verification result.
 *
 * @param success       Indicates whether the MFA challenge verification was successful.
 * @param failureReason If the verification failed, provides a reason for the failure (e.g., "Invalid code", "Challenge expired"). Null if the verification was successful.
 */
public record ChallengeProviderVerification(boolean success, String failureReason) {
    public static ChallengeProviderVerification verified() {
        return new ChallengeProviderVerification(true, null);
    }

    public static ChallengeProviderVerification failure(String reason) {
        return new ChallengeProviderVerification(false, reason);
    }
}
