package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaRecoveryCode;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaSettings;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserMfaRecoveryCodeRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.MfaRecoveryCodeService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
class MfaRecoveryCodeServiceImpl implements MfaRecoveryCodeService {

    private static final int RECOVERY_CODE_COUNT = 10;

    private final UserMfaRecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Generates a new set of recovery codes for the given MFA settings, stores their hashes in the database, and returns the plaintext codes. Any existing active recovery codes for the MFA settings are invalidated before generating new ones. The generated codes are normalized to uppercase and trimmed before hashing to ensure consistent verification.
     *
     * @param mfaSettings The MFA settings for which to generate recovery codes. Must not be null and must have a valid ID.
     * @return An array of plaintext recovery codes that were generated and stored. The caller is responsible for securely displaying these codes to the user, as they will not be retrievable after this method returns.
     */
    @Override
    @Transactional
    public String[] generateAndStoreRecoveryCodes(UserMfaSettings mfaSettings) {
        invalidateActiveRecoveryCodes(mfaSettings);

        String[] recoveryCodes = new RecoveryCodeGenerator().generateCodes(RECOVERY_CODE_COUNT);
        List<UserMfaRecoveryCode> hashedCodes = Arrays.stream(recoveryCodes)
                .map(this::hashRecoveryCode)
                .map(hash -> new UserMfaRecoveryCode(mfaSettings, hash))
                .toList();
        recoveryCodeRepository.saveAll(hashedCodes);
        return recoveryCodes;
    }

    /**
     * Verifies a recovery code against the stored hashes and consumes it if valid.
     *
     * @param mfaSettings  The MFA settings for which to verify the recovery code. Must not be null and must have a valid ID.
     * @param recoveryCode The recovery code to verify. Must not be null or blank.
     * @return true if the recovery code is valid and consumed, false otherwise.
     */
    @Override
    @Transactional
    public boolean verifyAndConsumeRecoveryCode(UserMfaSettings mfaSettings, String recoveryCode) {
        if (mfaSettings == null || recoveryCode == null || recoveryCode.isBlank()) {
            return false;
        }

        String normalizedCode = normalize(recoveryCode);
        Instant usedAt = Instant.now();
        return recoveryCodeRepository.findByMfaSettingsIdAndUsedFalseAndDeletedFalse(mfaSettings.getId())
                .stream()
                .filter(code -> passwordEncoder.matches(normalizedCode, code.getRecoveryCodeHash()))
                .anyMatch(code -> recoveryCodeRepository.consumeIfUnused(code.getId(), usedAt) == 1);
    }

    /**
     * Invalidates all active recovery codes for the given MFA settings.
     *
     * @param mfaSettings The MFA settings for which to invalidate recovery codes. Must not be null and must have a valid ID.
     */
    @Override
    @Transactional
    public void invalidateActiveRecoveryCodes(UserMfaSettings mfaSettings) {
        if (mfaSettings != null && mfaSettings.getId() != null) {
            recoveryCodeRepository.invalidateActiveCodes(mfaSettings.getId(), Instant.now());
        }
    }


    private String hashRecoveryCode(String recoveryCode) {
        return passwordEncoder.encode(normalize(recoveryCode));
    }

    private String normalize(String recoveryCode) {
        return recoveryCode.trim().toUpperCase(Locale.ROOT);
    }
}
