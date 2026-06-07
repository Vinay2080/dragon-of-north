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


/**
 * Default provider registry resolving MFA providers by type for orchestration flows.
 * <p>
 * This component serves as the central registry for all available MFA providers in the system. It maintains a mapping of provider types to their corresponding provider implementations, allowing for efficient retrieval and management of MFA providers based on their type. The registry is initialized with a list of all available providers, which are then organized into an unmodifiable map for quick access. It provides methods to retrieve a provider by type, get a list of available providers for a specific user, and check if a provider type is supported.
 */
@Component
public class DefaultMfaProviderRegistry implements MfaProviderRegistry {
    private final Map<ProviderType, MfaProvider> providersByType;

    /**
     * Constructs the provider registry with a list of available MFA providers. The providers are organized into an unmodifiable map keyed by their provider type for efficient retrieval.
     *
     * @param providers The list of MFA providers to be registered in the system.
     */
    public DefaultMfaProviderRegistry(List<MfaProvider> providers) {
        this.providersByType = providers.stream()
                .collect(Collectors.toUnmodifiableMap(MfaProvider::type, Function.identity()));
    }

    /**
     * Retrieves the MFA provider corresponding to the specified provider type. If the provider type is not supported, a {@link BusinessException} with {@link ErrorCode#INVALID_INPUT} is thrown.
     *
     * @param providerType The type of MFA provider to retrieve.
     * @return The MFA provider associated with the specified provider type.
     * @throws BusinessException if the provider type is not supported.
     */
    @Override
    public MfaProvider getProvider(ProviderType providerType) {
        MfaProvider provider = providersByType.get(providerType);
        if (provider == null) {
            throw new BusinessException(ErrorCode.MFA_INVALID_PROVIDER, "Unsupported MFA provider type: " + providerType);
        }

        return provider;
    }

    /**
     * Retrieves a list of available MFA providers for the specified user. The method filters the registered providers based on whether they are enabled for the given user and returns a list of those that are available.
     *
     * @param user The user for whom to retrieve available MFA providers.
     * @return A list of MFA providers that are enabled for the specified user.
     */
    @Override
    public List<MfaProvider> getAvailableProviders(AppUser user) {
        return providersByType.values().stream()
                .filter(provider -> provider.isEnabledFor(user))
                .toList();
    }

    /**
     * Checks if the registry supports the specified provider type. This method returns true if a provider for the given type is registered, and false otherwise.
     *
     * @param providerType The type of MFA provider to check for support.
     * @return true if the provider type is supported, false otherwise.
     */
    @Override
    public boolean supports(ProviderType providerType) {
        return providersByType.containsKey(providerType);
    }
}
