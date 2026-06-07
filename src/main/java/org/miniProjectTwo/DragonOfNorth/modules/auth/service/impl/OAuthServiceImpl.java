package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.GoogleTokenVerifierService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.OAuthService;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.SessionCreationSpec;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.enums.*;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Google OAuth flow orchestrator.
 * <p>
 * Validates Google token claims, matches/creates local provider linkage, and then delegates to
 * shared login issuance, so OAuth and local flows share session/token/MFA lifecycle semantics.
 */
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final GoogleTokenVerifierService tokenVerifierService;
    private final AppUserRepository appUserRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final RoleRepository roleRepository;
    private final AuthCommonServices authCommonServices;
    private final ProfileService profileService;
    private final AuditEventLogger auditEventLogger;
    private final UserStateValidator userStateValidator;

    /**
     * Authenticates a user with Google and establishes an application session.
     */
    @Override
    @Transactional
    public MfaOrchestrationResult authenticatedWithGoogle(String idToken, String expectedIdentifier, AuthRequestContext context, HttpServletResponse response) {
        return executeGoogleFlow(
                "auth.oauth.google.login",
                idToken,
                expectedIdentifier,
                context,
                response,
                this::findOrCreateUserForGoogleAuth
        );
    }

    /**
     * Creates or links an account with Google signup flow and establishes a session.
     */
    @Override
    @Transactional
    public MfaOrchestrationResult signupWithGoogle(String idToken, String expectedIdentifier, AuthRequestContext context, HttpServletResponse response) {
        return executeGoogleFlow(
                "auth.oauth.google.signup",
                idToken,
                expectedIdentifier,
                context,
                response,
                this::findOrCreateUserForSignup
        );
    }

    /**
     * Finds or creates a user for Google signup flow. This method first checks if there is an existing user linked to the Google provider ID. If found, it reactivates the user if needed and returns it. If not found, it checks for an existing user with the same email. If an email match is found and the account is deleted, it links the Google provider and reactivates the account. If the email match is active but not linked to Google, it throws an exception requiring the user to log in with a password before linking. If no matches are found, it creates a new user with the Google information.
     *
     * @param userInfo The OAuth user information extracted from the Google token.
     * @return The existing or newly created AppUser associated with the Google account.
     * @throws BusinessException If there is a conflict with existing accounts, that prevents linking or creation.
     */
    private AppUser findOrCreateUserForSignup(OAuthUserInfo userInfo) {
        Optional<UserAuthProvider> existingByProviderId = userAuthProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, userInfo.sub());
        if (existingByProviderId.isPresent()) {
            return reactivateForGoogleIfNeeded(existingByProviderId.get().getUser(), UserLifecycleOperation.GOOGLE_SIGNUP);
        }

        Optional<AppUser> existingByEmail = appUserRepository.findByEmailForUpdate(userInfo.email());
        if (existingByEmail.isPresent()) {
            AppUser user = existingByEmail.get();
            if (userStateValidator.isDeleted(user)) {
                linkGoogleProvider(user, userInfo.sub());
                return reactivateForGoogleIfNeeded(markGoogleIdentityVerified(user), UserLifecycleOperation.GOOGLE_SIGNUP);
            }
            if (userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), Provider.GOOGLE)) {
                throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN, "Google account mismatch. Please login again.");
            }
            throw new BusinessException(ErrorCode.OAUTH_LINK_CONFIRMATION_REQUIRED,
                    "Account already exists. Login with password before linking Google.");
        }

        return createNewUserWithRetry(userInfo);
    }

    /**
     * Reactivates the user for Google login if necessary.
     *
     * @param user      The user to reactivate.
     * @param operation The lifecycle operation being performed.
     * @return The updated user object.
     */
    private AppUser reactivateForGoogleIfNeeded(AppUser user, UserLifecycleOperation operation) {
        userStateValidator.validate(user, operation);
        if (!userStateValidator.isDeleted(user)) {
            return user;
        }

        user.setAppUserStatus(AppUserStatus.ACTIVE);
        user.setEmailVerified(true);
        profileService.ensureProfileExists(user.getId(), null);
        auditEventLogger.log("auth.reactivation", user.getId(), null, null, "success", "identifier_type=GOOGLE", null);
        return user;
    }

    /**
     * Links the Google provider to the given user.
     *
     * @param appUser The user to whom to link the Google provider.
     * @param providerId The ID of the Google provider.
     */
    private void linkGoogleProvider(AppUser appUser, String providerId) {
        if (userAuthProviderRepository.existsByUserIdAndProvider(appUser.getId(), Provider.GOOGLE)) {
            return;
        }
        UserAuthProvider provider = new UserAuthProvider();
        provider.setUser(appUser);
        provider.setProvider(Provider.GOOGLE);
        provider.setProviderId(providerId);
        userAuthProviderRepository.save(provider);
    }

    /**
     * Marks the Google identity as verified for the given user.
     *
     * @param user The user for whom to mark the Google identity as verified.
     * @return The updated user object.
     */
    private AppUser markGoogleIdentityVerified(AppUser user) {
        user.setEmailVerified(true);
        return user;
    }

    /**
     * Creates a new user with the provided Google information. This method attempts to create a new user record in the database with the email and provider information from Google. If a DataIntegrityViolationException occurs (e.g., due to a concurrent creation of a user with the same email), it retries by checking for existing provider linkage and email matches again, ensuring that only one user is created for the given Google account even under concurrent requests.
     *
     * @param userInfo The OAuth user information extracted from the Google token.
     * @return The newly created AppUser associated with the Google account, or an existing user if a concurrent creation was detected.
     * @throws BusinessException If user creation fails due to reasons other than concurrent creation, or if there are issues linking the Google provider.
     */
    private AppUser createNewUserWithRetry(OAuthUserInfo userInfo) {
        try {
            AppUser newUser = new AppUser();
            newUser.setEmail(userInfo.email());
            newUser.setEmailVerified(true);
            newUser.setPassword(null);
            newUser.setAppUserStatus(AppUserStatus.ACTIVE);
            newUser.setFailedLoginAttempts(0);
            newUser.setAccountLocked(false);

            Role userRole = roleRepository.findByRoleName(RoleName.USER)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "USER role not found"));
            newUser.setRoles(Set.of(userRole));

            AppUser savedUser = appUserRepository.save(newUser);
            linkGoogleProvider(savedUser, userInfo.sub());
            profileService.ensureProfileExists(savedUser.getId(), userInfo);
            return savedUser;

        } catch (DataIntegrityViolationException e) {
            auditEventLogger.log("auth.oauth.google.signup", null, null, null, "retry", "concurrent user creation detected", null);

            Optional<UserAuthProvider> byProviderId = userAuthProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, userInfo.sub());
            if (byProviderId.isPresent()) {
                return byProviderId.get().getUser();
            }

            AppUser existingUser = appUserRepository.findByEmailForUpdate(userInfo.email())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_CREATION_FAILED, "Failed to create user"));
            existingUser.setEmailVerified(true);
            existingUser.setPassword(null);
            linkGoogleProvider(existingUser, userInfo.sub());
            return reactivateForGoogleIfNeeded(existingUser, UserLifecycleOperation.GOOGLE_SIGNUP);
        }
    }

    /**
     * Executes the common flow for both Google login and signup operations. This method verifies the Google token, resolves or creates the corresponding AppUser using the provided user resolver function, synchronizes the user's Google profile, finalizes authentication by issuing a session, and records the outcome of the operation for auditing purposes.
     *
     * @param eventName          The name of the audit event to record (e.g., "auth.oauth.google.login" or "auth.oauth.google.signup").
     * @param idToken            The ID token received from Google to be verified.
     * @param expectedIdentifier An optional expected identifier (such as email) to validate against the token claims.
     * @param context            The authentication request context containing additional information about the request.
     * @param response           The HTTP response object used to set authentication cookies or headers.
     * @param userResolver       A function that takes OAuthUserInfo and returns an AppUser, used to resolve or create the user based on the token information.
     * @return An MfaOrchestrationResult indicating the result of the authentication process, including any required MFA steps.
     * @throws BusinessException If any step of the process fails, such as token verification failure, user resolution issues, or authentication finalization problems. The exception will contain details about the failure for auditing and client feedback purposes.
     */
    private MfaOrchestrationResult executeGoogleFlow(String eventName,
                                                     String idToken,
                                                     String expectedIdentifier,
                                                     AuthRequestContext context,
                                                     HttpServletResponse response,
                                                     GoogleUserResolver userResolver) {
        UUID auditUserId = null;
        try {
            OAuthUserInfo userInfo = verifyGoogleIdentity(idToken, expectedIdentifier);
            AppUser appUser = userResolver.resolve(userInfo);
            synchronizeGoogleProfile(appUser, userInfo);
            auditUserId = appUser.getId();
            MfaOrchestrationResult result = finalizeAuthentication(appUser, context, response);
            recordOauthSuccess(eventName, auditUserId, context);
            return result;
        } catch (BusinessException exception) {
            recordOauthFailure(eventName, auditUserId, context, exception);
            throw exception;
        }
    }

    /**
     * Verifies the Google identity using the provided ID token and expected identifier.
     *
     * @param idToken            The ID token received from Google.
     * @param expectedIdentifier The expected identifier (e.g., email) to validate against the token.
     * @return The verified OAuth user information.
     * @throws BusinessException If the ID token is invalid or the expected identifier does not match.
     */
    private OAuthUserInfo verifyGoogleIdentity(String idToken, String expectedIdentifier) {
        OAuthUserInfo userInfo = tokenVerifierService.verifyToken(idToken);
        validateExpectedIdentifier(userInfo, expectedIdentifier);
        return userInfo;
    }

    /**
     * Synchronizes the Google profile information for the given user.
     *
     * @param appUser  The user for whom to synchronize profile information.
     * @param userInfo The OAuth user information extracted from the Google token.
     */
    private void synchronizeGoogleProfile(AppUser appUser, OAuthUserInfo userInfo) {
        profileService.syncGoogleAvatar(appUser.getId(), userInfo);
    }

    /**
     * Finalizes the authentication process for the given user.
     *
     * @param appUser  The authenticated user.
     * @param context  The authentication request context.
     * @param response The HTTP response object.
     * @return The result of the MFA orchestration.
     */
    private MfaOrchestrationResult finalizeAuthentication(AppUser appUser, AuthRequestContext context, HttpServletResponse response) {
        updateLoginInfo(appUser);

        return authCommonServices.issueLoginSession(
                appUser,
                SessionCreationSpec.fromAppUser(appUser, "oauth"),
                response,
                context
        );
    }

    private void recordOauthSuccess(String eventName, UUID userId, AuthRequestContext context) {
        auditEventLogger.log(eventName, userId, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    private void recordOauthFailure(String eventName, UUID userId, AuthRequestContext context, BusinessException exception) {
        auditEventLogger.log(eventName, userId, context.deviceId(), context.ipAddress(), "failure", exception.getMessage(), context.requestId());
    }

    /**
     * Validates the expected identifier (e.g., email) against the information in the OAuth token. If an expected identifier is provided, it checks that it matches the email claim from the token. If there is a mismatch, it throws a BusinessException indicating that the OAuth identity does not match the entered email and prompts the user to log in with Google using the same email.
     *
     * @param userInfo           The OAuth user information extracted from the Google token.
     * @param expectedIdentifier The expected identifier (such as email) to validate against the token claims.
     * @throws BusinessException If the expected identifier is provided and does not match the email claim from the token.
     */
    private void validateExpectedIdentifier(OAuthUserInfo userInfo, String expectedIdentifier) {
        if (expectedIdentifier == null || expectedIdentifier.trim().isEmpty()) {
            return;
        }

        String normalizedExpected = expectedIdentifier.trim().toLowerCase();
        String oauthEmail = userInfo.email() == null ? "" : userInfo.email().trim().toLowerCase();

        if (!normalizedExpected.equals(oauthEmail)) {
            throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN,
                    "OAuth identity does not match entered email. Please login with Google using the same email.");
        }
    }

    /**
     * Updates the login information for the given user.
     *
     * @param user The user for whom to update login information.
     */
    private void updateLoginInfo(AppUser user) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
    }

    /**
     * Finds or creates a user for Google signup flow. This method first checks if there is an existing user linked to the Google provider ID. If found, it reactivates the user if needed and returns it. If not found, it checks for an existing user with the same email. If an email match is found and the account is deleted, it links the Google provider and reactivates the account. If the email match is active but not linked to Google, it throws an exception requiring the user to log in with a password before linking. If no matches are found, it creates a new user with the Google information.
     *
     * @param userInfo The OAuth user information extracted from the Google token.
     * @return The existing or newly created AppUser associated with the Google account.
     * @throws BusinessException If there is a conflict with existing accounts, that prevents linking or creation.
     */
    private AppUser findOrCreateUserForGoogleAuth(OAuthUserInfo userInfo) {
        Optional<UserAuthProvider> existingByProviderId = userAuthProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, userInfo.sub());
        if (existingByProviderId.isPresent()) {
            return reactivateForGoogleIfNeeded(existingByProviderId.get().getUser(), UserLifecycleOperation.GOOGLE_LOGIN);
        }

        Optional<AppUser> existingByEmail = appUserRepository.findByEmailForUpdate(userInfo.email());
        if (existingByEmail.isPresent()) {
            AppUser user = existingByEmail.get();
            userStateValidator.validate(user, UserLifecycleOperation.GOOGLE_LOGIN);
            linkGoogleProvider(user, userInfo.sub());
            return reactivateForGoogleIfNeeded(markGoogleIdentityVerified(user), UserLifecycleOperation.GOOGLE_LOGIN);
        }

        return createNewUserWithRetry(userInfo);
    }

    /**
     * Functional interface for resolving an AppUser from OAuthUserInfo. This is used to abstract the user resolution logic for different OAuth flows (e.g., login vs. signup) while reusing the common execution flow in the executeGoogleFlow method.
     */
    @FunctionalInterface
    private interface GoogleUserResolver {
        AppUser resolve(OAuthUserInfo userInfo);
    }
}
