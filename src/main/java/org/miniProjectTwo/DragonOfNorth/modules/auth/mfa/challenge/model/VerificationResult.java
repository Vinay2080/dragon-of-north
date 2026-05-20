package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model;

import java.util.UUID;

/**
 * Outcome of an MFA verification attempt.
 */
public record VerificationResult(
        UUID userId,
        String primaryAmr,
        boolean success
) {
}

