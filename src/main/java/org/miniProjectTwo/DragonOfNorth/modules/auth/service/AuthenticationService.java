package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Strategy interface for identifier-specific authentication flows.
 */
public interface AuthenticationService {

    /**
     * Returns the identifier type handled by this implementation.
     */
    IdentifierType supports();

    /**
     * Looks up the current account state for an identifier.
     */
    AppUserStatusFinderResponse getUserStatus(String identifier);

    /**
     * Starts sign-up for the provided identifier payload.
     */
    AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request);

    /**
     * Completes sign-up after verification.
     */
    AppUserStatusFinderResponse completeSignUp(String identifier);

}
