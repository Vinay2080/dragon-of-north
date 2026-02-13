package org.miniProjectTwo.DragonOfNorth.services.auth;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.AuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.NOT_EXIST;
import static org.miniProjectTwo.DragonOfNorth.enums.ErrorCode.USER_NOT_FOUND;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.PHONE;

/**
 * Phone-based authentication service implementation.
 * <p>
 * Handles user registration, status checking, and verification completion for phone
 * identifiers. Manages password encryption, phone verification flags, and database
 * operations. Critical for phone authentication flow and user lifecycle management.
 *
 * @see AuthenticationServiceResolver for service selection
 * @see AuthCommonServices for shared authentication logic
 */
@Service
@RequiredArgsConstructor

public class PhoneAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthCommonServices authCommonServices;

    /**
     * Returns PHONE identifier type for service routing.
     * <p>
     * Used by AuthenticationServiceResolver to select this implementation
     * for phone-based authentication requests.
     *
     * @return PHONE identifier type
     */
    @Override
    public IdentifierType supports() {
        return PHONE;
    }

    /**
     * Retrieves user registration status for phone identifier.
     * <p>
     * Queries a database for phone-based user status or returns NOT_EXIST
     * if the user is not found. Critical for frontend authentication flow guidance.
     *
     * @param identifier phone number to check
     * @return user status response or NOT_EXIST status
     */
    @Override
    public AppUserStatusFinderResponse getUserStatus(String identifier) {
        return appUserRepository
                .findAppUserStatusByPhone(identifier).map(AppUserStatusFinderResponse::new)
                .orElseGet(() -> new AppUserStatusFinderResponse(NOT_EXIST));
    }

    /**
     * Creates a new user account with phone identifier.
     * <p>
     * Encrypts password, sets CREATED status, and persists user to a database.
     * Does not assign roles or verify phone - handled in completion flow.
     * Critical for user registration initiation.
     *
     * @param request sign-up request with phone and password
     * @return created user status response
     */
    @Override
    public AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request) {
        AppUser appUser = new AppUser();
        appUser.setPhone(request.identifier());
        appUser.setPassword(passwordEncoder.encode(request.password()));
        appUser.setAppUserStatus(CREATED);
        appUserRepository.save(appUser);
        return getUserStatus(request.identifier());
    }

    /**
     * Completes user registration with phone verification.
     * <p>
     * Updates user status to VERIFIED, assigns a default USER role,
     * and persists changes. Does not set a phone verification flag.
     * Critical for enabling full authentication capabilities.
     *
     * @param identifier phone number to complete registration
     * @return updated user status response
     * @throws BusinessException if a user isn't found or already verified
     */
    @Override
    public AppUserStatusFinderResponse completeSignUp(String identifier) {
        AppUser appUser = appUserRepository.findByPhone(identifier).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        authCommonServices.updateUserStatus(appUser.getAppUserStatus(), appUser);
        authCommonServices.assignDefaultRole(appUser);
        appUserRepository.save(appUser);
        return getUserStatus(identifier);
    }


}
