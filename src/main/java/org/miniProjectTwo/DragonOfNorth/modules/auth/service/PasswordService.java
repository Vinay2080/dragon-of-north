package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.validation.Valid;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordChangeRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordResetConfirmRequest;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Handles all password-related operations (encode/change/reset flows).
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

