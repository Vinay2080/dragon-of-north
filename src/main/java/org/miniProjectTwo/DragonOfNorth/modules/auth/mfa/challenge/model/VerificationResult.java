package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model;

import java.util.UUID;

/**
 * Outcome of an MFA verification attempt.
 */
public record VerificationResult(
        UUID userId,
        String primaryAmr,
        boolean success,
        FailureReason failureReason
) {
    public static VerificationResult success(UUID userId, String primaryAmr) {
        return new VerificationResult(userId, primaryAmr, true, FailureReason.NONE);
    }

    public static VerificationResult failure(UUID userId, String primaryAmr, FailureReason failureReason) {
        return new VerificationResult(userId, primaryAmr, false, failureReason);
    }

    public enum FailureReason {
        NONE,
        CHALLENGE_EXPIRED_OR_MISSING,
        CHALLENGE_BUSY_OR_REPLAY,
        CHALLENGE_LOCKED_OUT,
        CONTEXT_MISMATCH,
        PROVIDER_MISMATCH,
        INVALID_VERIFICATION_CODE,
        CHALLENGE_CONSUME_RACE
    }
}
