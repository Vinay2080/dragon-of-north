package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model;

import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.time.Instant;

/**
 * Result of an MFA verification attempt.
 * <p>
 * Encapsulates the success status, provider type, verification timestamp, and failure reason (if applicable) of an MFA verification attempt. Used by the MFA service to communicate the outcome of MFA verification back to the authentication flow, which can then determine whether to proceed with session creation or reject the authentication attempt based on the verification result.
 *
 * @param success       Indicates whether the MFA verification was successful.
 * @param providerType  The type of MFA provider used for the verification attempt (e.g., TOTP, SMS, email).
 * @param verifiedAt    If the verification was successful, provides the timestamp of when the verification occurred. Null if the verification failed.
 * @param failureReason If the verification failed, provides a reason for the failure (e.g., "Invalid code", "Verification expired"). Null if the verification was successful.
 */
public record VerifyResult(
        boolean success,
        ProviderType providerType,
        Instant verifiedAt,
        String failureReason
) {
    public static VerifyResult success(ProviderType providerType) {
        return new VerifyResult(true, providerType, Instant.now(), null);
    }

    public static VerifyResult failure(ProviderType providerType, String failureReason) {
        return new VerifyResult(false, providerType, null, failureReason);
    }
}
