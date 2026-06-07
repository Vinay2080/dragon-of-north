package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.registry;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider.MfaProvider;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

import java.util.List;

/**
 * Registry for MFA providers.
 *
 * <p>Provides lookup and discovery of available MFA providers. This allows the system to support
 * multiple MFA methods (e.g., TOTP, SMS, email) modularly.</p>
 */
public interface MfaProviderRegistry {
    /**
     * Retrieves the MFA provider implementation for the specified provider type.
     *
     * @param providerType The type of MFA provider to retrieve (e.g., TOTP, SMS).
     * @return The corresponding {@link MfaProvider} implementation.
     * @throws IllegalArgumentException if no provider is found for the given type.
     */
    MfaProvider getProvider(ProviderType providerType);

    /**
     * Retrieves a list of available MFA providers that are enabled for the given user.
     *
     * @param user The user for whom to retrieve available MFA providers.
     * @return A list of {@link MfaProvider} instances that are enabled for the user.
     */
    List<MfaProvider> getAvailableProviders(AppUser user);

    /**
     * Checks if the registry supports the specified provider type.
     *
     * @param providerType The type of MFA provider to check for support.
     * @return True if the provider type is supported, false otherwise.
     */
    boolean supports(ProviderType providerType);
}
