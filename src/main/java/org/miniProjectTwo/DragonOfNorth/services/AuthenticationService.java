package org.miniProjectTwo.DragonOfNorth.services;

import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;

public interface AuthenticationService {
    AppUserStatusFinderResponse statusFinder(AppUserStatusFinderRequest username);



}
