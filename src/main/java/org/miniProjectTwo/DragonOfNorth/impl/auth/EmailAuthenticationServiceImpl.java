package org.miniProjectTwo.DragonOfNorth.impl.auth;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.NOT_EXIST;
import static org.miniProjectTwo.DragonOfNorth.enums.ErrorCode.USER_NOT_FOUND;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.EMAIL;

/**
 * Email-based authentication service implementation.
 * Handles user registration, status checking, and verification completion for email
 * identifiers. Manages password encryption, email verification flags, and database
 * operations. Critical for email authentication flow and user lifecycle management.
 *
 * @see AuthenticationServiceResolver for service selection
 * @see AuthCommonServices for shared authentication logic
 */
@Service
@RequiredArgsConstructor
public class EmailAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthCommonServices authCommonServices;


    /**
     * Returns EMAIL identifier type for service routing.
     * Used by AuthenticationServiceResolver to select this implementation
     * for email-based authentication requests.
     *
     * @return EMAIL identifier type
     */
    @Override
    public IdentifierType supports() {
        return EMAIL;
    }

    /**
     * Retrieves user registration status for email identifier.
     * <p>
     * Queries a database for email-based user status or returns NOT_EXIST
     * if user not found. Critical for frontend authentication flow guidance.
     *
     * @param identifier email address to check
     * @return user status response or NOT_EXIST status
     */
    @Override
    public AppUserStatusFinderResponse getUserStatus(String identifier) {
        return appUserRepository
                .findAppUserStatusByEmail(identifier).map(AppUserStatusFinderResponse::new)
                .orElseGet(() -> new AppUserStatusFinderResponse(NOT_EXIST));
    }

    /**
     * Creates a new user account with email identifier.
     * <p>
     * Encrypts password, sets CREATED status, and persists user to a database.
     * Does not assign roles or verify email - handled in completion flow.
     * Critical for user registration initiation.
     *
     * @param request sign-up request with email and password
     * @return created user status response
     */
    @Override
    public AppUserStatusFinderResponse signUpUser(AppUserSignUpRequest request) {
        AppUser user = new AppUser();
        user.setEmail(request.identifier());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAppUserStatus(CREATED);
        appUserRepository.save(user);
        return getUserStatus(request.identifier());
    }


    /**
     * Completes user registration with email verification.
     * <p>
     * Updates user status to VERIFIED, assigns a default USER role, sets
     * an email verification flag, and persists changes transactionally.
     * Critical for enabling full authentication capabilities.
     *
     * @param identifier email address to complete registration
     * @return updated user status response
     * @throws BusinessException if a user isn't found or already verified
     */
    @Transactional
    @Override
    public AppUserStatusFinderResponse completeSignUp(String identifier) {
        AppUser appUser = appUserRepository.findByEmail(identifier).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        authCommonServices.updateUserStatus(appUser.getAppUserStatus(), appUser);
        authCommonServices.assignDefaultRole(appUser);
        appUser.setEmailVerified(true);
        appUserRepository.save(appUser);
        return getUserStatus(identifier);
    }

}

