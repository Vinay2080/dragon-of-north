package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.PasswordResetConfirmRequest;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;

public interface AuthCommonServices {

    void login(String identifier, String password, HttpServletResponse response, HttpServletRequest request, String deviceId);

    void refreshToken(HttpServletRequest request, HttpServletResponse response, String deviceId);

    void assignDefaultRole(AppUser appUser);

    void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser);

    void logoutUser(HttpServletRequest request, HttpServletResponse response, String deviceId);

    void requestPasswordResetOtp(String identifier, IdentifierType identifierType);

    void resetPassword(PasswordResetConfirmRequest request);
}
