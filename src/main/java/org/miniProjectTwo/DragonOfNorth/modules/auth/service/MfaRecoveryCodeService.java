package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaSettings;

public interface MfaRecoveryCodeService {

    String[] generateAndStoreRecoveryCodes(UserMfaSettings mfaSettings);

    boolean verifyAndConsumeRecoveryCode(UserMfaSettings mfaSettings, String recoveryCode);

    void invalidateActiveRecoveryCodes(UserMfaSettings mfaSettings);
}
