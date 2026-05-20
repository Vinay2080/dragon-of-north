package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.registry;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider.MfaProvider;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DefaultMfaProviderRegistry implements MfaProviderRegistry {
    private final Map<ProviderType, MfaProvider> providersByType;

    public DefaultMfaProviderRegistry(List<MfaProvider> providers) {
        this.providersByType = providers.stream()
                .collect(Collectors.toUnmodifiableMap(MfaProvider::type, Function.identity()));
    }

    @Override
    public MfaProvider getProvider(ProviderType providerType) {
        MfaProvider provider = providersByType.get(providerType);
        if (provider == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Unsupported MFA provider type: " + providerType);
        }

        return provider;
    }

    @Override
    public List<MfaProvider> getAvailableProviders(AppUser user) {
        return providersByType.values().stream()
                .filter(provider -> provider.isEnabledFor(user))
                .toList();
    }

    @Override
    public boolean supports(ProviderType providerType) {
        return providersByType.containsKey(providerType);
    }
}
