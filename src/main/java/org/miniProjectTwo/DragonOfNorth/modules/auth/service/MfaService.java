package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupConfirmResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupResponse;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;

/**
 * Service interface for handling Multi-Factor Authentication (MFA) operations.
 *
 * <p>This service provides methods for initiating MFA setup, confirming MFA setup, and verifying MFA codes during authentication processes. It abstracts the underlying MFA provider implementations and allows for flexible integration of various MFA methods (e.g., TOTP, SMS, email).</p>
 */
public interface MfaService {

    /**
     * Initiates the MFA setup process for a user. This method generates the necessary information and resources required for the user to set up MFA, such as generating a TOTP secret or sending an SMS code. The response includes details that the client can use to complete the MFA setup process.
     *
     * @param context The authentication request context containing information about the user and the MFA setup request.
     * @return An {@link MfaSetupResponse} containing the details needed for the client to complete MFA setup.
     */
    MfaSetupResponse requestMfaSetup(AuthRequestContext context);

    /**
     * Confirms the MFA setup process by verifying the provided MFA code. This method is called after the user has completed the initial setup steps (e.g., scanning a QR code for TOTP or receiving an SMS code) and submits the MFA code for verification. If the code is valid, the MFA setup is confirmed and enabled for the user.
     *
     * @param context The authentication request context containing information about the user and the MFA setup confirmation request.
     * @param code    The MFA code submitted by the user for confirmation. This should be a non-null, non-blank string that meets any length requirements defined by the specific MFA provider.
     * @return An {@link MfaSetupConfirmResponse} indicating whether the MFA setup confirmation was successful or if there were any errors during verification.
     */
    MfaSetupConfirmResponse confirmMfaSetup(AuthRequestContext context, @NotNull @Length String code);

    /**
     * Verifies an MFA code for the given user.
     *
     * @param appUser The user for whom to verify the MFA code.
     * @param code    The MFA code to verify.
     * @return true if the code is valid, false otherwise.
     */
    boolean verifyMfaCode(AppUser appUser, @NotNull @Length String code);

    /**
     * Verifies a TOTP code for the given user.
     *
     * @param appUser The user for whom to verify the TOTP code.
     * @param code    The TOTP code to verify.
     * @return true if the code is valid, false otherwise.
     */
    boolean verifyTotpCode(AppUser appUser, @NotNull @Length String code);

    /**
     * Verifies a recovery code for the given user.
     *
     * @param appUser The user for whom to verify the recovery code.
     * @param code    The recovery code to verify.
     * @return true if the code is valid, false otherwise.
     */
    boolean verifyRecoveryCode(AppUser appUser, @NotNull @Length String code);

    /**
     * Disables MFA for the user associated with the given authentication context. This method is typically called when a user chooses to turn off MFA or when an administrator needs to disable MFA for a user. It should handle all necessary cleanup, such as invalidating existing MFA credentials and updating the user's MFA status in the system.
     *
     * @param context The authentication request context containing information about the user for whom MFA should be disabled.
     */
    void disableMfa(AuthRequestContext context);
}
