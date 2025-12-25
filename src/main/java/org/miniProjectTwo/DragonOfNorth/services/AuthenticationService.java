package org.miniProjectTwo.DragonOfNorth.services;

import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;

import java.util.UUID;

public interface AuthenticationService {

    IdentifierType supports();

    AppUserStatusFinderResponse getUserStatus(String identifier);

    AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request);

    void updateStatusById(UUID uuid, AppUserStatus appUserStatus);

    void assignDefaultRole(AppUser appUser);

    void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser);

    AppUserStatusFinderResponse completeSignUp(String identifier);


}
