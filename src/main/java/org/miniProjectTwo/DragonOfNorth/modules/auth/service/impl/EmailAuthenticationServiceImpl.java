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
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
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
 **/
@Service
@RequiredArgsConstructor
public class EmailAuthenticationServiceImpl implements AuthenticationService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final AuthCommonServices authCommonServices;
    private final MeterRegistry meterRegistry;
    private final AuditEventLogger auditEventLogger;
    private final ProfileService profileService;


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
     * Queries the database for an email-based user's current status.
     * <p>
     * If the user is not present in the database, this method returns a
     * {@code AppUserStatusFinderResponse.notFound()} result. For existing
     * users the response contains the current {@code AppUser.appUserStatus}
     * (for example {@code ACTIVE} or {@code LOCKED/DELETED}) along with
     * provider information and the email-verified flag. The frontend
     * consumes this information to decide the next authentication step.
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
                        userAuthProviderRepository.findAllByUserId(user.getId())
                                .stream()
                                .map(UserAuthProvider::getProvider)
                                .distinct()
                                .toList(),
                        user.isEmailVerified(),
                        user.getAppUserStatus()
                ))
                .orElseGet(AppUserStatusFinderResponse::notFound);
    }

    /**
     * Creates a new user account with email identifier.
     * <p>
     * Encrypts the provided password, persists a new user record and sets the
     * initial account state. In the current model the service sets the user
     * {@code AppUser.appUserStatus} to {@code ACTIVE} and leaves
     * {@code emailVerified} as {@code false}. Role assignment and any
     * additional activation steps are handled in the completion flow.
     * This method is critical for initiating the user registration process.
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
     * Finalizes registration after verification (for example, after
     * successful OTP/email verification). This method assigns a default
     * USER role, sets the {@code emailVerified} flag to {@code true} and
     * persists changes transactionally so the user can fully authenticate.
     * Note: the method does not assume the use of intermediary statuses like
     * {@code CREATED} or {@code VERIFIED} — it updates verification state and
     * performs role provisioning according to the current status model
     * (e.g., the account may already be {@code ACTIVE}).
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
            profileService.createProfile(appUser.getId(), null);
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
