package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

public interface AuthenticationService {

    IdentifierType supports();

    AppUserStatusFinderResponse getUserStatus(String identifier);

    AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request);

    AppUserStatusFinderResponse completeSignUp(String identifier);

}
