package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordChangeRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordResetConfirmRequest;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Shared authentication operations used across identifier strategies.
 */
public interface AuthCommonServices {

    /**
     * Authenticates user credentials and issues auth cookies/session.
     */
    void login(String identifier, String password, HttpServletResponse response, AuthRequestContext context);

    /**
     * Rotates refresh/access tokens for the active device.
     */
    void refreshToken(String refreshToken, HttpServletResponse response, AuthRequestContext context);

    /**
     * Ensures the user has the default role if no roles are assigned.
     */
    void assignDefaultRole(AppUser appUser);

    /**
     * Updates account status.
     */
    void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser);

    /**
     * Revokes current-device session and clears auth cookies.
     */
    void logoutUser(String refreshToken, HttpServletResponse response, AuthRequestContext context);

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
}
