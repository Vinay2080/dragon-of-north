package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.registry;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider.MfaProvider;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.util.List;

public interface MfaProviderRegistry {
    MfaProvider getProvider(ProviderType providerType);

    List<MfaProvider> getAvailableProviders(AppUser user);

    boolean supports(ProviderType providerType);
}
