package org.miniProjectTwo.DragonOfNorth.services;

import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

public interface AuthenticationService {

    IdentifierType supports();
    AppUserStatusFinderResponse statusFinder(AppUserStatusFinderRequest request);



}
