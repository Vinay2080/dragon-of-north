package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthenticationService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.USER_NOT_FOUND;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.PHONE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

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
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final AuthCommonServices authCommonServices;
    private final MeterRegistry meterRegistry;
    private final AuditEventLogger auditEventLogger;

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
        meterRegistry.counter("auth.status_lookup.requested").increment();
        return appUserRepository.findByPhone(identifier)
                .map(user -> new AppUserStatusFinderResponse(
                        true,
                        userAuthProviderRepository.findAllByUserId(user.getId()).stream().map(UserAuthProvider::getProvider).distinct().toList(),
                        user.isEmailVerified(),
                        user.getAppUserStatus()
                ))
                .orElseGet(AppUserStatusFinderResponse::notFound);
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
        appUser.setAppUserStatus(ACTIVE);
        try {
            AppUser savedUser = appUserRepository.save(appUser);
            UserAuthProvider localProvider = new UserAuthProvider();
            localProvider.setUser(savedUser);
            localProvider.setProvider(LOCAL);
            userAuthProviderRepository.save(localProvider);
            meterRegistry.counter("auth.signup.success").increment();
            auditEventLogger.log("auth.signup", null, null, null, "success", "identifier_type=PHONE", null);
            return getUserStatus(request.identifier());
        } catch (RuntimeException ex) {
            meterRegistry.counter("auth.signup.failure").increment();
            auditEventLogger.log("auth.signup", null, null, null, "failure", ex.getMessage(), null);
            throw ex;
        }
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
        try {
            AppUser appUser = appUserRepository.findByPhone(identifier).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

            authCommonServices.assignDefaultRole(appUser);
            appUserRepository.save(appUser);
            meterRegistry.counter("auth.signup.complete.success").increment();
            auditEventLogger.log("auth.signup.complete", appUser.getId(), null, null, "success", "identifier_type=PHONE", null);
            return getUserStatus(identifier);
        } catch (RuntimeException ex) {
            meterRegistry.counter("auth.signup.complete.failure").increment();
            auditEventLogger.log("auth.signup.complete", null, null, null, "failure", ex.getMessage(), null);
            throw ex;
        }
    }


}
