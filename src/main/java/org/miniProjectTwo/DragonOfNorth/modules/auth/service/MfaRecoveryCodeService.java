package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaSettings;

/**
 * Service for managing MFA recovery codes.
 *
 * <p>Provides functionality to generate, verify, consume, and invalidate MFA recovery codes. Recovery
 * codes are one-time use codes that can be used to bypass MFA challenges when the primary method is
 * unavailable.</p>
 */
public interface MfaRecoveryCodeService {

    /**
     * Generates a new set of recovery codes for the given MFA settings, stores their hashes in the database, and returns the plaintext codes. Any existing active recovery codes for the MFA settings are invalidated before generating new ones. The generated codes are normalized to uppercase and trimmed before hashing to ensure consistent verification.
     *
     * @param mfaSettings The MFA settings for which to generate recovery codes. Must not be null and must have a valid ID.
     * @return An array of plaintext recovery codes that were generated and stored. The caller is responsible for securely displaying these codes to the user, as they will not be retrievable after this method returns.
     */
    String[] generateAndStoreRecoveryCodes(UserMfaSettings mfaSettings);

    /**
     * Verifies a recovery code against the stored hashes and consumes it if valid.
     *
     * @param mfaSettings  The MFA settings for which to verify the recovery code. Must not be null and must have a valid ID.
     * @param recoveryCode The recovery code to verify. Must not be null or blank.
     * @return true if the recovery code is valid and consumed, false otherwise.
     */
    boolean verifyAndConsumeRecoveryCode(UserMfaSettings mfaSettings, String recoveryCode);

    /**
     * Invalidates all active recovery codes for the given MFA settings.
     *
     * @param mfaSettings The MFA settings for which to invalidate recovery codes. Must not be null and must have a valid ID.
     */
    void invalidateActiveRecoveryCodes(UserMfaSettings mfaSettings);
}
