package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.MfaContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

public interface MfaProvider {
    ProviderType type();

    boolean isEnabledFor(AppUser user);

    boolean allowsLoginChallenge();

    boolean allowsStepUp();

    VerifyResult verify(AppUser user, String code, MfaContext context);
}
