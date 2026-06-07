package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model;

import java.util.UUID;

/**
 * Outcome of an MFA verification attempt.
 * <pre>
 * - On success, contains the user id, primary authentication method (for context), MFA method used, and a success flag.
 * - On failure, contains the user id, primary authentication method, failure reason, and a success flag set to false. The MFA method is null in this case since the verification did not succeed.
 * </pre>
 * This result is used to determine the next steps in the authentication flow, such as issuing tokens on success or returning appropriate error responses on failure.
 * The failure reason provides granular insight into why the verification failed, which can be used for security monitoring, user feedback, and to inform client-side handling of different failure scenarios (e.g. prompting for a new code if expired, or showing a lockout message if the account is locked). The primary authentication method is included for context to help correlate the MFA verification attempt with the initial authentication step, which can be useful for auditing and debugging purposes.
 *
 */
public record VerificationResult(
        UUID userId,
        String primaryAmr,
        String mfaMethodAmr,
        boolean success,
        FailureReason failureReason
) {
    public static VerificationResult success(UUID userId, String primaryAmr, String mfaMethodAmr) {
        return new VerificationResult(userId, primaryAmr, mfaMethodAmr, true, FailureReason.NONE);
    }

    public static VerificationResult failure(UUID userId, String primaryAmr, FailureReason failureReason) {
        return new VerificationResult(userId, primaryAmr, null, false, failureReason);
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
