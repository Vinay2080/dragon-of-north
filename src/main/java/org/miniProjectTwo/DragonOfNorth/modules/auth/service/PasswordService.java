package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.validation.Valid;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordChangeRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordResetConfirmRequest;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Password lifecycle service for local credentials.
 * <p>
 * Covers reset initiation, reset confirmation, authenticated password rotation, and password hashing.
 * Implementations must preserve non-enumerating behavior for public reset flows and revoke/contain
 * existing sessions when credential integrity changes.
 */
public interface PasswordService {

    /**
     * Starts password-reset OTP flow for the provided identifier.
     */
    void requestPasswordResetOtp(String identifier, IdentifierType identifierType);

    /**
     * Confirms password reset with OTP verification.
     */
    void resetPassword(PasswordResetConfirmRequest request);

    /**
     * Changes password for the authenticated user.
     */
    void changePassword(@Valid PasswordChangeRequest request);

    /**
     * Encodes a raw password for persistence.
     */
    String encodePassword(String rawPassword);
}

