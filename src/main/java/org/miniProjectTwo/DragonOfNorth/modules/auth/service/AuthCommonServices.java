package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordResetConfirmRequest;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

public interface AuthCommonServices {

    void login(String identifier, String password, HttpServletResponse response, HttpServletRequest request, String deviceId);

    void refreshToken(HttpServletRequest request, HttpServletResponse response, String deviceId);

    void assignDefaultRole(AppUser appUser);

    void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser);

    void logoutUser(HttpServletRequest request, HttpServletResponse response, String deviceId);

    void requestPasswordResetOtp(String identifier, IdentifierType identifierType);

    void resetPassword(PasswordResetConfirmRequest request);
}
