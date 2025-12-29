package org.miniProjectTwo.DragonOfNorth.services;

import org.miniProjectTwo.DragonOfNorth.dto.auth.request.RefreshTokenRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AuthenticationResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.RefreshTokenResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;

public interface AuthCommonServices {

    AuthenticationResponse login(String identifier, String password);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    void assignDefaultRole(AppUser appUser);

    void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser);
}
