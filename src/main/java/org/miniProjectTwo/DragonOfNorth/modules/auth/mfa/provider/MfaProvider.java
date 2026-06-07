package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.MfaContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

/**
 * Interface for MFA providers that can be used in the authentication process.
 * <p>
 * Defines the contract for MFA providers, including methods for checking a provider type, enabling status, and performing verification. Implementations of this interface will provide the specific logic for different MFA methods (e.g., TOTP, SMS, email) and will be used by the MFA service to handle MFA challenges during authentication. The interface includes methods to determine if the provider is enabled for a given user, whether it allows login challenges or step-up authentication, and to verify MFA codes in the context of an ongoing authentication flow.
 */
public interface MfaProvider {
    /**
     * Returns the type of MFA provider.
     * <p> This method is used to identify the specific MFA provider implementation (e.g., TOTP, SMS, email) and route verification logic accordingly. The provider type is a key attribute that allows the MFA service to determine which provider to use for a given MFA challenge based on the user's enrolled MFA methods and the authentication context.
     *
     * @return The {@link ProviderType} associated with this MFA provider.
     */
    ProviderType type();

    /**
     * Checks if this MFA provider is enabled for the specified user.
     * <p> This method allows the MFA service to determine whether a particular MFA provider should be offered as an option for MFA verification during authentication. The implementation should check the user's enrolled MFA methods and any relevant user or system settings to determine if this provider is available for use. For example, a TOTP provider might check if the user has a TOTP secret configured, while an SMS provider might check if the user has a valid phone number on file and has enabled SMS-based MFA.
     *
     * @param user The user for whom to check if this MFA provider is enabled. The implementation may use attributes of the user (e.g., enrolled MFA methods, account status) to determine if the provider should be offered.
     * @return {@code true} if the provider is enabled for the user, {@code false} otherwise.
     */
    boolean isEnabledFor(AppUser user);

    /**
     * Indicates whether this MFA provider allows login challenges.
     * <p> This method is used to determine if the MFA provider can be used in
     * the initial MFA challenge step during login. If a provider allows login challenges, it means that it can be offered as an option for MFA verification immediately after the primary authentication step (e.g., password verification) if the user's context requires MFA. Providers that do not allow login challenges may only be used for step-up authentication after the user has already been authenticated and is performing a sensitive action that requires additional verification.
     *
     * @return {@code true} if the provider allows login challenges, {@code false} otherwise.
     */
    boolean allowsLoginChallenge();

    /**
     * Indicates whether this MFA provider allows step-up authentication.
     * <p> This method is used to determine if the MFA provider can be used for step-up authentication, which is an additional MFA verification step that can be triggered after the user has already been authenticated with their primary credentials. Step-up authentication is typically used for sensitive actions (e.g., changing account settings, performing high-risk transactions) where additional verification is required to ensure the user's identity. If a provider allows step-up authentication, it means that it can be offered as an option for MFA verification in these scenarios, even if it is not used in the initial login challenge.
     *
     * @return {@code true} if the provider allows step-up authentication, {@code false} otherwise.
     */
    boolean allowsStepUp();

    /**
     * Verifies the provided MFA code for the specified user in the context of an ongoing authentication flow.
     * <p> This method is responsible for performing the actual verification of the MFA code submitted by the user during an MFA challenge. The implementation should validate the code against the expected value for the user and provider type, taking into account the context of the authentication flow (e.g., whether it's a login challenge or step-up authentication). The verification result should indicate whether the code was valid and, if not, provide a reason for the failure (e.g., "Invalid code", "Code expired"). The MFA context can be used to provide additional information about the authentication flow, such as the primary authentication method used, the user's session state, or any relevant metadata that may influence the verification logic.
     */
    VerifyResult verify(AppUser user, String code, MfaContext context);
}
