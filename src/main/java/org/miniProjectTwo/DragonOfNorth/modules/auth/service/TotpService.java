package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;

/**
 * Service for handling Time-based One-Time Password (TOTP) operations.
 *
 * <p>This service provides methods for generating TOTP secrets, creating QR codes for user setup,
 * and validating TOTP codes during authentication.</p>
 */
public interface TotpService {
    /**
     * Generates a new TOTP secret for a user. This secret is used to configure the user's TOTP authenticator app (e.g., Google Authenticator, Authy).
     *
     * @return A newly generated TOTP secret as a string.
     */
    String generateSecret();

    /**
     * Generates a QR code for the given TOTP secret and user. The QR code can be scanned by the user's TOTP authenticator app to set up their TOTP MFA.
     *
     * @param secret  The TOTP secret for which to generate the QR code.
     * @param appUser The user for whom to generate the QR code. This may be used to include user-specific information in the QR code (e.g., username, issuer).
     * @return A string representation of the QR code (e.g., a data URL or an ASCII representation) that can be rendered on the client side for scanning.
     */
    String generateQrCode(String secret, AppUser appUser);

    /**
     * Validates a TOTP code against the provided secret. This method checks if the given TOTP code is valid for the current time window based on the secret.
     *
     * @param secret The TOTP secret to validate against.
     * @param code   The TOTP code to validate. This should be a non-null, non-blank string that meets any length requirements defined by the TOTP standard (typically 6 digits).
     * @return true if the TOTP code is valid for the given secret, false otherwise.
     */
    boolean isValidCode(String secret, String code);
}
