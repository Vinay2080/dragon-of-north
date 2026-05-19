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

    @Override
    @Transactional
    public boolean verifyAndConsumeRecoveryCode(UserMfaSettings mfaSettings, String recoveryCode) {
        if (mfaSettings == null || recoveryCode == null || recoveryCode.isBlank()) {
            return false;
        }

        String normalizedCode = normalize(recoveryCode);
        return recoveryCodeRepository.findByMfaSettingsIdAndUsedFalseAndDeletedFalse(mfaSettings.getId())
                .stream()
                .filter(code -> passwordEncoder.matches(normalizedCode, code.getRecoveryCodeHash()))
                .findFirst()
                .map(code -> {
                    code.markUsed(Instant.now());
                    recoveryCodeRepository.save(code);
                    return true;
                })
                .orElse(false);
    }

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
