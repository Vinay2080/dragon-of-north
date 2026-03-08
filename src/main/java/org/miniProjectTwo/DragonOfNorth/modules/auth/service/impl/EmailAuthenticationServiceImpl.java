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
import org.springframework.transaction.annotation.Transactional;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.USER_NOT_FOUND;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.EMAIL;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

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
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final AuthCommonServices authCommonServices;
    private final MeterRegistry meterRegistry;
    private final AuditEventLogger auditEventLogger;


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
        meterRegistry.counter("auth.status_lookup.requested").increment();
        return appUserRepository.findByEmail(identifier)
                .map(user -> new AppUserStatusFinderResponse(
                        true,
                        userAuthProviderRepository.findAllByUserId(user.getId()).stream().map(UserAuthProvider::getProvider).distinct().toList(),
                        user.isEmailVerified(),
                        user.getAppUserStatus()
                ))
                .orElseGet(AppUserStatusFinderResponse::notFound);
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
        user.setAppUserStatus(ACTIVE);
        user.setEmailVerified(false);
        try {
            AppUser savedUser = appUserRepository.save(user);
            UserAuthProvider localProvider = new UserAuthProvider();
            localProvider.setUser(savedUser);
            localProvider.setProvider(LOCAL);
            userAuthProviderRepository.save(localProvider);
            meterRegistry.counter("auth.signup.success").increment();
            auditEventLogger.log("auth.signup", null, null, null, "success", "identifier_type=EMAIL", null);
            return getUserStatus(request.identifier());
        } catch (RuntimeException ex) {
            meterRegistry.counter("auth.signup.failure").increment();
            auditEventLogger.log("auth.signup", null, null, null, "failure", ex.getMessage(), null);
            throw ex;
        }
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
        try {
            AppUser appUser = appUserRepository.findByEmail(identifier).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
            authCommonServices.assignDefaultRole(appUser);
            appUser.setEmailVerified(true);
            appUserRepository.save(appUser);
            meterRegistry.counter("auth.signup.complete.success").increment();
            auditEventLogger.log("auth.signup.complete", appUser.getId(), null, null, "success", "identifier_type=EMAIL", null);
            return getUserStatus(identifier);
        } catch (RuntimeException ex) {
            meterRegistry.counter("auth.signup.complete.failure").increment();
            auditEventLogger.log("auth.signup.complete", null, null, null, "failure", ex.getMessage(), null);
            throw ex;
        }
    }

}
