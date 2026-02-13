package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;

public interface AuthCommonServices {

    void login(String identifier, String password, HttpServletResponse response);

    void refreshToken(HttpServletRequest request, HttpServletResponse response);

    void assignDefaultRole(AppUser appUser);

    void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser);

    void logoutUser(HttpServletRequest request, HttpServletResponse response);
}
