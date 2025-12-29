package org.miniProjectTwo.DragonOfNorth.services;

import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

public interface AuthenticationService {

    IdentifierType supports();

    AppUserStatusFinderResponse getUserStatus(String identifier);

    AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request);

    AppUserStatusFinderResponse completeSignUp(String identifier);

}
