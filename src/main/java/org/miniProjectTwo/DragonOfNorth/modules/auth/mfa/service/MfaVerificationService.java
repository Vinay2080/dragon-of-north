package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.service;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.MfaContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

public interface MfaVerificationService {
    VerifyResult verifyAtLogin(AppUser user, ProviderType providerType, String code, MfaContext context);

    VerifyResult verifyForStepUp(AppUser user, ProviderType providerType, String code, MfaContext context);
}
