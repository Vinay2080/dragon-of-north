package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.service;

import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.MfaContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.model.VerifyResult;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

/**
 * Service responsible for verifying MFA codes during login and step-up authentication flows.
 *
 * <p>This service delegates to the appropriate {@link org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider.MfaProvider}
 * based on the requested provider type. It also enforces that the provider supports the requested
 * verification context (login vs. step-up) and that it is enabled for the user.</p>
 */
public interface MfaVerificationService {
    /**
     * Verifies the provided MFA code for the specified user and provider type during the login process.
     *
     * @param user         The user attempting to verify MFA.
     * @param providerType The type of MFA provider being verified (e.g., TOTP, SMS).
     * @param code         The MFA code submitted by the user for verification.
     * @param context      Additional context for the verification attempt (optional).
     * @return A {@link VerifyResult} indicating whether the verification was successful or failed, along with any relevant failure reasons.
     */
    VerifyResult verifyAtLogin(AppUser user, ProviderType providerType, String code, MfaContext context);

    /**
     * Verifies the provided MFA code for the specified user and provider type during a step-up authentication scenario.
     *
     * @param user         The user attempting to verify MFA for step-up authentication.
     * @param providerType The type of MFA provider being verified (e.g., TOTP, SMS).
     * @param code         The MFA code submitted by the user for verification.
     * @param context      Additional context for the verification attempt (optional).
     * @return A {@link VerifyResult} indicating whether the verification was successful or failed, along with any relevant failure reasons.
     */
    VerifyResult verifyForStepUp(AppUser user, ProviderType providerType, String code, MfaContext context);
}
