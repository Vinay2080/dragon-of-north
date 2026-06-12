package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.VerificationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.MfaChallengeService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrator;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.provider.MfaProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.registry.MfaProviderRegistry;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.SessionTokenIssuer;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.SessionCreationSpec;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.security.service.SessionAccessTokenIssuer;
import org.miniProjectTwo.DragonOfNorth.shared.enums.*;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.IdentifierNormalizer;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

/**
 * Core authentication orchestration service used by all primary authentication flows.
 * <p>
 * Coordinates credential authentication, user lifecycle validation, MFA orchestration,
 * session creation, JWT issuance, cookie management, refresh token rotation,
 * logout handling, and account deletion.
 * <p>
 * Local login, OAuth authentication, passwordless authentication, and MFA completion
 * ultimately converge through this service to ensure consistent security behavior.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AuthCommonServiceImpl implements AuthCommonServices {

    private final AuthenticationManager authenticationManager;
    private final JwtServices jwtServices;
    private final RoleRepository roleRepository;
    private final SessionService sessionService;
    private final SessionRepository sessionRepository;
    private final SessionAccessTokenIssuer sessionAccessTokenIssuer;
    private final SessionTokenIssuer sessionTokenIssuer;
    private final MfaOrchestrator mfaOrchestrator;
    private final MfaChallengeService mfaChallengeService;
    private final MfaProviderRegistry mfaProviderRegistry;
    private final MeterRegistry meterRegistry;
    private final AppUserRepository appUserRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final AuditEventLogger auditEventLogger;
    private final UserStateValidator userStateValidator;
    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * Handles the local login process for a user. This method performs the following steps:
     * 1. Normalizes the provided identifier (email or phone).
     * 2. Retrieves the user associated with the normalized identifier.
     * 3. Validates the user's state to ensure they are allowed to log in.
     * 4. Ensures the user is registered via a local provider.
     * 5. Authenticates the user using the authentication manager.
     * 6. Ensures the user's identifier is verified.
     * 7. Issues a login session and handles MFA orchestration if required.
     * 8. Records login success or failure events for auditing and metrics.
     *
     * @param identifier User identifier, which can be an email or phone number
     * @param password   User's password for authentication
     * @param response   HTTP response object used to set authentication cookies
     * @param context    Additional context for the authentication request, including device and IP information
     * @return MfaOrchestrationResult indicating whether MFA challenges are required after successful credential authentication
     */
    @Override
    public MfaOrchestrationResult login(String identifier, String password, HttpServletResponse response, AuthRequestContext context) {
        String normalizedIdentifier = normalizeIdentifier(identifier);
        UUID auditUserId = null;
        try {
            AppUser user = findUserForLogin(normalizedIdentifier);
            auditUserId = user.getId();

            userStateValidator.validate(user, UserLifecycleOperation.LOCAL_LOGIN);
            ensureLocalProvider(user);
            AppUser authenticatedUser = authenticateUser(normalizedIdentifier, password);
            ensureIdentifierVerified(user, normalizedIdentifier);

            MfaOrchestrationResult result = issueLoginSession(authenticatedUser, SessionCreationSpec.fromAppUser(authenticatedUser, "pwd"), response, context);
            if (!result.challengeRequired()) {
                recordLoginSuccess(authenticatedUser.getId(), context);
            }
            return result;
        } catch (AuthenticationException | BusinessException exception) {
            recordLoginFailure(auditUserId, context, exception.getMessage());
            throw exception;
        }
    }

    /**
     * Refreshes the authentication session by rotating the refresh token. This method performs the following steps:
     * 1. Validates the provided refresh token and device ID.
     * 2. Rotates the refresh tokens to prevent reuse and enhance security.
     * 3. Issues new authentication cookies with the new tokens.
     * 4. Records refresh success or failure events for auditing and metrics.
     *
     * @param oldRefreshToken The refresh token to be rotated
     * @param response        HTTP response object used to set new authentication cookies
     * @param context         Additional context for the authentication request, including device and IP information
     */
    @Override
    public void refreshToken(String oldRefreshToken, HttpServletResponse response, AuthRequestContext context) {
        try {
            validateRefreshRequest(oldRefreshToken, context);
            TokenRefreshData refreshData = rotateRefreshTokens(oldRefreshToken, context.deviceId());
            writeAuthCookies(response, refreshData.toLoginTokens());
            recordRefreshSuccess(refreshData.userId(), context);
        } catch (BusinessException exception) {
            clearRefreshTokenCookie(response);
            recordRefreshFailure(context, exception.getMessage());
            throw exception;
        }
    }

    /**
     * Assigns the default USER role to a newly registered user if they do not have any roles assigned. This ensures that every user has at least basic permissions and access rights defined by the USER role. The method checks if the user already has any roles, and if not, it retrieves the USER role from the database and assigns it to the user. If the USER role is not found in the database, a BusinessException is thrown indicating that the role is missing.
     * @param appUser The user to whom the default role should be assigned. The method will check if this user has any roles and assign the USER role if none is present.
     */
    @Override
    public void assignDefaultRole(AppUser appUser) {
        if (!appUser.hasAnyRoles()) {
            Role userRole = roleRepository.findByRoleName(RoleName.USER)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, RoleName.USER.toString()));
            appUser.getRoles().add(userRole);
        }
    }

    /**
     * Updates the user's status to the specified AppUserStatus. This method is used to change the status of a user account, such as activating, deactivating, or marking it as deleted. The method takes in the new status and the user object, and it sets the user's status to the new value. This is typically used in user lifecycle management operations where the account status needs to be updated based on certain actions or events (e.g., account deletion, suspension).
     *
     * @param appUserStatus The new status to be assigned to the user (e.g., ACTIVE, INACTIVE, DELETED).
     * @param appUser The user whose status is being updated. The method will set this user's status to the provided appUserStatus.
     */
    @Override
    public void updateUserStatus(AppUserStatus appUserStatus, AppUser appUser) {
        appUser.setAppUserStatus(appUserStatus);
    }

    /**
     * Logs out the user by revoking the session associated with the provided refresh token and device ID. This method performs the following steps:
     * 1. Validates the logout request by checking the refresh token and device ID.
     * 2. Extracts the user ID from the refresh token for auditing purposes.
     * 3. Validates the user's state to ensure they are allowed to log out.
     * 4. Revokes the session associated with the refresh token and device ID.
     * 5. Clears authentication cookies from the response to remove client-side session state.
     * 6. Records logout success or failure events for auditing and metrics.
     *
     * @param refreshToken The refresh token associated with the session to be revoked
     * @param response HTTP response object used to clear authentication cookies
     * @param context Additional context for the logout request, including device and IP information
     */
    @Override
    public void logoutUser(String refreshToken, HttpServletResponse response, AuthRequestContext context) {
        validateLogoutRequest(refreshToken, context);

        UUID userId = extractLogoutUserId(refreshToken, context);
        try {
            userStateValidator.validate(findUserById(userId), UserLifecycleOperation.SESSION_REVOKE_CURRENT);
            sessionService.revokeSession(refreshToken, context.deviceId());
        } catch (BusinessException exception) {
            recordLogoutFailure(userId, context, exception.getMessage());
            clearAuthCookies(response);
            return;
        }

        clearAuthCookies(response);
        recordLogoutSuccess(userId, context);
    }

    /**
     * Deletes the authenticated user's account by marking it as DELETED and revoking all active sessions. This method performs the following steps:
     * 1. Retrieves the currently authenticated user.
     * 2. Validates the user's state to ensure they are allowed to delete their account.
     * 3. Updates the user's status to DELETED in the database.
     * 4. Revokes all active sessions for the user to log them out from all devices.
     * 5. Clears authentication cookies from the response to remove client-side session state.
     * 6. Records account deletion success events for auditing and metrics.
     *
     * @param response HTTP response object used to clear authentication cookies
     * @param context Additional context for the account deletion request, including device and IP information
     */
    @Override
    @Transactional
    public void deleteAccount(HttpServletResponse response, AuthRequestContext context) {
        AppUser appUser = findAuthenticatedUser();
        userStateValidator.validate(appUser, UserLifecycleOperation.ACCOUNT_DELETION);
        appUser.setAppUserStatus(AppUserStatus.DELETED);
        appUserRepository.save(appUser);
        sessionService.revokeAllSessionsByUserId(appUser.getId());
        clearAuthCookies(response);
        recordAccountDeletionSuccess(appUser.getId(), context);
    }
    private void recordAccountDeletionSuccess(UUID userId, AuthRequestContext context) {
        meterRegistry.counter("auth.account_delete.success").increment();
        auditEventLogger.log("auth.account_delete", userId, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    /**
     * Retrieves the currently authenticated user from the security context. This method checks the authentication object in the security context to ensure that a user is authenticated. If no authentication is found or if the principal is null, it throws a BusinessException indicating that access is denied. If an authenticated user is found, it resolves the user's ID from the authentication principal and retrieves the corresponding AppUser from the database. If the user cannot be found, it throws a BusinessException indicating that the user was not found.
     *
     * @return The currently authenticated AppUser
     * @throws BusinessException if no authenticated user is found, or if the user cannot be retrieved from the database
     */
    @Override
    public AppUser findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "User not authenticated");
        }

        UUID userId = resolveAuthenticatedUserId(authentication.getPrincipal());
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Completes the login process for a user after successful credential authentication. This method performs the following steps:
     * 1. Ensures that the user's identifier (email or phone) is verified.
     * 2. Issues a login session and handles MFA orchestration if required.
     * 3. Records login success events if no MFA challenges are required.
     *
     * @param appUser The authenticated user for whom the login is being completed
     * @param identifier The identifier used for login (email or phone)
     * @param response HTTP response object used to set authentication cookies
     * @param context Additional context for the authentication request, including device and IP information
     * @return MfaOrchestrationResult indicating whether MFA challenges are required after successful credential authentication
     */
    @Override
    public MfaOrchestrationResult completeLogin(AppUser appUser, String identifier, HttpServletResponse response, AuthRequestContext context) {
        ensureIdentifierVerified(appUser, identifier);
        MfaOrchestrationResult result = issueLoginSession(appUser, SessionCreationSpec.fromAppUser(appUser, "passwordless"), response, context);
        if (!result.challengeRequired()) {
            recordLoginSuccess(appUser.getId(), context);
        }
        return result;
    }

    /**
     * Sets the access token in the HTTP response as a secure, HttpOnly cookie with appropriate attributes.
     *
     * @param response HTTP response object used to set the access token cookie
     * @param token    The access token to be set in the cookie
     */
    public void setAccessToken(HttpServletResponse response, String token) {
        Cookie accessCookie = new Cookie("access_token", token);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(cookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(60 * 15);
        accessCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(accessCookie);
    }

    /**
     * Sets the refresh token in the HTTP response as a secure, HttpOnly cookie with appropriate attributes.
     *
     * @param response HTTP response object used to set the refresh token cookie
     * @param token    The refresh token to be set in the cookie
     */
    public void setRefreshToken(HttpServletResponse response, String token) {
        Cookie refreshCookie = new Cookie("refresh_token", token);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(cookieSecure);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
        refreshCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(refreshCookie);
    }

    /**
     * Issues a login session for the authenticated user. This method orchestrates the login process, including handling MFA challenges if required.
     *
     * @param appUser The authenticated user for whom the login session is being issued
     * @param creationSpec The specification for creating the login session
     * @param response HTTP response object used to set authentication cookies
     * @param context Additional context for the authentication request, including device and IP information
     * @return MfaOrchestrationResult indicating whether MFA challenges are required after successful credential authentication
     */
    @Override
    public MfaOrchestrationResult issueLoginSession(AppUser appUser, SessionCreationSpec creationSpec, HttpServletResponse response, AuthRequestContext context) {
        MfaOrchestrationResult result = mfaOrchestrator.orchestrateLogin(appUser, creationSpec.primaryAmr(), context);
        if (result.challengeRequired()) {
            return result;
        }

        SessionTokenIssuer.LoginTokens loginTokens = sessionTokenIssuer.issueLoginSession(
                appUser,
                creationSpec,
                context.ipAddress(),
                context.deviceId(),
                context.userAgent()
        );
        writeAuthCookies(response, loginTokens);
        return result;
    }

    /**
     * Completes the MFA challenge login process for a user after successful verification of the MFA code. This method performs the following steps:
     * 1. Verifies the MFA challenge and consumes it if valid.
     * 2. Retrieves the authenticated user based on the verification result.
     * 3. Issues a login session and sets authentication cookies.
     * 4. Records the login success event.
     *
     * @param challengeId The ID of the MFA challenge to complete
     * @param code The MFA code provided by the user
     * @param providerType The type of MFA provider used
     * @param response HTTP response object used to set authentication cookies
     * @param context Additional context for the authentication request, including device and IP information
     * @return VerificationResult indicating the outcome of the MFA challenge completion
     * @throws BusinessException if the MFA challenge is invalid, or if the user cannot be authenticated
     */
    @Override
    public VerificationResult completeMfaChallengeLogin(String challengeId, String code, org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType providerType, HttpServletResponse response, AuthRequestContext context) {
        VerificationResult verificationResult = mfaChallengeService.verifyAndConsume(challengeId, providerType, code, context);
        if (!verificationResult.success() || verificationResult.userId() == null || verificationResult.primaryAmr() == null || verificationResult.mfaMethodAmr() == null) {
            throw switch (verificationResult.failureReason()) {
                case CHALLENGE_LOCKED_OUT -> new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
                case CHALLENGE_EXPIRED_OR_MISSING, CHALLENGE_BUSY_OR_REPLAY, CHALLENGE_CONSUME_RACE ->
                        new BusinessException(ErrorCode.MFA_CHALLENGE_FAILED);
                case PROVIDER_MISMATCH, CONTEXT_MISMATCH, INVALID_VERIFICATION_CODE, NONE ->
                        new BusinessException(ErrorCode.MFA_INVALID_CODE);
            };
        }

        AppUser appUser = findUserById(verificationResult.userId());
        userStateValidator.validate(appUser, UserLifecycleOperation.LOCAL_LOGIN);

        SessionCreationSpec creationSpec = new SessionCreationSpec(
                verificationResult.primaryAmr(),
                false,
                java.time.Instant.now(),
                verificationResult.mfaMethodAmr()
        );
        SessionTokenIssuer.LoginTokens loginTokens = sessionTokenIssuer.issueLoginSession(
                appUser,
                creationSpec,
                context.ipAddress(),
                context.deviceId(),
                context.userAgent()
        );
        writeAuthCookies(response, loginTokens);
        recordLoginSuccess(appUser.getId(), context);
        return verificationResult;
    }

    /**
     * Issues a step-up MFA challenge for the authenticated user. This method performs the following steps:
     * 1. Validates that the user is authenticated and that the session ID is provided.
     * 2. Asserts that the session is live and owned by the authenticated user.
     * 3. Retrieves the available MFA methods for the user that allow step-up challenges.
     * 4. Creates a step-up MFA challenge using the MfaChallengeService.
     * 5. Records the issuance of the step-up challenge for auditing purposes.
     *
     * @param user The authenticated user for whom the step-up challenge is being issued
     * @param sessionId The ID of the authenticated session to which the step-up challenge will be bound
     * @param context Additional context for the authentication request, including device and IP information
     * @return MfaChallenge containing the details of the issued step-up challenge
     * @throws BusinessException if the user is not authenticated, if the session ID is missing, if the session is not live or not owned by the user, or if no MFA methods are available for step-up challenges
     */
    @Override
    public MfaChallenge issueStepUpChallenge(AppUser user, UUID sessionId, AuthRequestContext context) {
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "User not authenticated");
        }
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session ID missing for step-up challenge");
        }

        assertLiveSessionOwnership(sessionId, user.getId());

        List<ProviderType> availableMethods =
                mfaProviderRegistry.getAvailableProviders(user).stream()
                        .filter(MfaProvider::allowsStepUp)
                        .map(MfaProvider::type)
                        .toList();

        if (availableMethods.isEmpty()) {
            throw new BusinessException(ErrorCode.MFA_REQUIRED, "No MFA methods available for step-up");
        }

        MfaChallenge challenge = mfaChallengeService.createStepUpChallenge(user.getId(), sessionId, context, availableMethods);
        auditEventLogger.log(SecurityAuditEvent.AUTH_MFA_STEPUP_CHALLENGE_ISSUED,
                user.getId(), context.deviceId(), context.ipAddress(), "success", null, context.requestId());
        return challenge;
    }

    /**
     * Completes the step-up MFA challenge for the authenticated user. This method performs the following steps:
     * 1. Validates that the session is live and owned by the authenticated user.
     * 2. Verifies the MFA challenge and consumes it if valid.
     * 3. Retrieves the authenticated user based on the verification result.
     * 4. Refreshes the session with the new MFA verification timestamp and method.
     * 5. Issues a new access token for the session and sets it in the response cookies.
     * 6. Records the completion of the step-up challenge for auditing purposes.
     *
     * @param challengeId The ID of the MFA challenge to complete
     * @param providerType The type of MFA provider used
     * @param code The MFA code provided by the user
     * @param sessionId The ID of the authenticated session associated with the step-up challenge
     * @param response HTTP response object used to set new authentication cookies
     * @param context Additional context for the authentication request, including device and IP information
     * @throws BusinessException if the MFA challenge is invalid, if the session is not live or not owned by the user, or if the user cannot be authenticated
     */
    @Override
    public void completeStepUpMfaChallenge(String challengeId,
                                           ProviderType providerType,
                                           String code,
                                           UUID sessionId,
                                           HttpServletResponse response,
                                           AuthRequestContext context) {
        assertLiveSessionOwnership(sessionId, resolveAuthenticatedUserId(Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal()));
        VerificationResult verificationResult = mfaChallengeService.verifyAndConsume(challengeId, providerType, code, context, sessionId);

        if (!verificationResult.success() || verificationResult.userId() == null || verificationResult.mfaMethodAmr() == null) {
            throw switch (verificationResult.failureReason()) {
                case CHALLENGE_LOCKED_OUT -> new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
                case CHALLENGE_EXPIRED_OR_MISSING, CHALLENGE_BUSY_OR_REPLAY, CHALLENGE_CONSUME_RACE ->
                        new BusinessException(ErrorCode.MFA_CHALLENGE_FAILED);
                case PROVIDER_MISMATCH, CONTEXT_MISMATCH, INVALID_VERIFICATION_CODE, NONE ->
                        new BusinessException(ErrorCode.MFA_INVALID_CODE);
            };
        }

        AppUser appUser = findUserById(verificationResult.userId());
        userStateValidator.validate(appUser, UserLifecycleOperation.LOCAL_LOGIN);

        Instant verifiedAt = Instant.now();
        Session updatedSession = sessionService.refreshMfaVerifiedAt(sessionId, appUser.getId(), verifiedAt, verificationResult.mfaMethodAmr());

        Set<Role> roles = roleRepository.findRolesById(appUser.getId());
        String newAccessToken = sessionAccessTokenIssuer.mintAccessToken(updatedSession, roles);
        setAccessToken(response, newAccessToken);

        meterRegistry.counter("auth.mfa.step_up.success").increment();
        auditEventLogger.log(SecurityAuditEvent.AUTH_MFA_STEPUP_COMPLETED,
                appUser.getId(), context.deviceId(), context.ipAddress(), "success",
                "session_id=" + sessionId, context.requestId());
    }

    /**
     * Asserts that the provided session is live and owned by the specified user.
     *
     * @param sessionId Session identifier
     * @param userId    User identifier
     */
    private void assertLiveSessionOwnership(UUID sessionId, UUID userId) {
        boolean live = sessionRepository.existsLiveSessionForUser(sessionId, userId, Instant.now());
        if (!live) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session not found or no longer live");
        }
    }

    /**
     * Resolves the authenticated user's identifier from the provided principal object.
     *
     * @param principal Authentication principal object
     * @return User identifier
     */
    private UUID resolveAuthenticatedUserId(Object principal) {
        return switch (principal) {
            case AppUserDetails appUserDetails -> appUserDetails.getAppUser().getId();
            case SecurityPrincipal securityPrincipal -> securityPrincipal.userId();
            case UUID id -> id;
            case String raw when !raw.isBlank() -> parseAuthenticatedUserId(raw);
            case null, default -> throw new BusinessException(ErrorCode.ACCESS_DENIED, "User not authenticated");
        };
    }

    /**
     * Parses the authenticated user's identifier from a raw string representation.
     *
     * @param raw Raw string representation of the user identifier
     * @return User identifier
     */
    private UUID parseAuthenticatedUserId(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Invalid authentication principal");
        }
    }

    /**
     * Validates the logout request by checking the refresh token and device ID.
     *
     * @param refreshToken Refresh token
     * @param context      Authentication request context
     */
    private void validateLogoutRequest(String refreshToken, AuthRequestContext context) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            recordLogoutFailure(null, context, "refresh token missing");
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "refresh token missing");
        }
        if (context.deviceId() == null || context.deviceId().trim().isEmpty()) {
            recordLogoutFailure(null, context, "device ID missing");
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "device ID missing");
        }
    }

    /**
     * Extracts the user identifier from the refresh token.
     *
     * @param refreshToken Refresh token
     * @param context      Authentication request context
     * @return User identifier
     */
    private UUID extractLogoutUserId(String refreshToken, AuthRequestContext context) {
        try {
            return jwtServices.extractUserId(refreshToken);
        } catch (BusinessException businessException) {
            recordLogoutFailure(null, context, "invalid refresh token: " + businessException.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Invalid refresh token");
        }
    }

    /**
     * Records a logout failure event with metrics and audit logging.
     *
     * @param userId  User identifier
     * @param context Authentication request context
     * @param message Failure message
     */
    private void recordLogoutFailure(UUID userId, AuthRequestContext context, String message) {
        meterRegistry.counter("auth.logout.failure").increment();
        auditEventLogger.log(SecurityAuditEvent.AUTH_LOGOUT_FAILED, userId, context.deviceId(), context.ipAddress(), "failure", message, context.requestId());
    }

    /**
     * Clears authentication cookies from the HTTP response.
     *
     * @param response HTTP response object used to clear authentication cookies
     */
    private void clearAuthCookies(HttpServletResponse response) {
        clearRefreshTokenCookie(response);
        clearAccessTokenCookie(response);
    }

    /**
     * Records a successful logout event with metrics and audit logging.
     *
     * @param userId  User identifier
     * @param context Authentication request context
     */
    private void recordLogoutSuccess(UUID userId, AuthRequestContext context) {
        meterRegistry.counter("auth.logout.success").increment();
        auditEventLogger.log(SecurityAuditEvent.AUTH_LOGOUT_SUCCESS, userId, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    /**
     * Clears the access token cookie from the HTTP response by setting an expired cookie with the same name and appropriate attributes. This method is used to remove the access token from the client's browser when a user logs out or when the access token is rotated. It ensures that the cookie is properly cleared by setting its value to an empty string, marking it as HttpOnly and Secure, and setting its max age to 0.
     *
     * @param response HTTP response object used to clear the access token cookie
     */
    public void clearAccessTokenCookie(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(cookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        accessCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(accessCookie);
    }

    /**
     * Validates the refresh request by checking the old refresh token and device ID.
     *
     * @param oldRefreshToken Old refresh token
     * @param context           Authentication request context
     */
    private void validateRefreshRequest(String oldRefreshToken, AuthRequestContext context) {
        if (oldRefreshToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token missing");
        }
        ensureDeviceIdPresent(context.deviceId());
    }

    /**
     * Rotates refresh tokens for a user session.
     *
     * @param oldRefreshToken Old refresh token
     * @param deviceId        Device identifier
     * @return TokenRefreshData object
     * @throws BusinessException if token rotation fails
     */
    private TokenRefreshData rotateRefreshTokens(String oldRefreshToken, String deviceId) {
        UUID userIdFromOldToken = jwtServices.extractUserId(oldRefreshToken);
        userStateValidator.validate(findUserById(userIdFromOldToken), UserLifecycleOperation.SESSION_ROTATE_REFRESH);
        String newRefreshToken = jwtServices.generateRefreshToken(userIdFromOldToken);
        Session session = sessionService.validateAndRotateSession(oldRefreshToken, newRefreshToken, deviceId);
        Set<Role> roles = roleRepository.findRolesById(session.getAppUser().getId());
        String newAccessToken = sessionAccessTokenIssuer.mintAccessToken(session, roles);
        return new TokenRefreshData(session.getAppUser().getId(), newAccessToken, newRefreshToken);
    }

    /**
     * Records a successful refresh token operation with metrics and audit logging.
     *
     * @param userId  User identifier
     * @param context Authentication request context
     */
    private void recordRefreshSuccess(UUID userId, AuthRequestContext context) {
        meterRegistry.counter("auth.refresh.success").increment();
        auditEventLogger.log(SecurityAuditEvent.AUTH_REFRESH_SUCCESS, userId, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    /**
     * Clears the refresh token cookie from the HTTP response by setting an expired cookie with the same name and appropriate attributes. This method is used to remove the refresh token from the client's browser when a user logs out or when the refresh token is rotated. It ensures that the cookie is properly cleared by setting its value to an empty string, marking it as HttpOnly and Secure, and setting its max age to 0.
     *
     * @param response HTTP response object used to clear the refresh token cookie
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie refrehCookie = new Cookie("refresh_token", "");
        refrehCookie.setHttpOnly(true);
        refrehCookie.setSecure(cookieSecure);
        refrehCookie.setPath("/");
        refrehCookie.setMaxAge(0);
        refrehCookie.setAttribute("SameSite", cookieSameSite);
        response.addCookie(refrehCookie);
    }

    /**
     * Records a failure during a refresh token operation with metrics and audit logging.
     *
     * @param context Authentication request context
     * @param message Failure message
     */
    private void recordRefreshFailure(AuthRequestContext context, String message) {
        meterRegistry.counter("auth.refresh.failure").increment();
        auditEventLogger.log(SecurityAuditEvent.AUTH_REFRESH_FAILED, null, context.deviceId(), context.ipAddress(), "failure", message, context.requestId());
    }

    /**
     * Ensures that the device ID is present in the request context.
     *
     * @param deviceId Device identifier
     * @throws BusinessException if device ID is missing
     */
    private void ensureDeviceIdPresent(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "device ID missing");
        }
    }

    /**
     * Retrieves an AppUser by their unique identifier.
     *
     * @param userId User identifier
     * @return AppUser object
     * @throws BusinessException if a user is not found
     */
    private AppUser findUserById(UUID userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        return identifier.contains("@")
                ? IdentifierNormalizer.normalizeEmail(identifier)
                : IdentifierNormalizer.normalizePhone(identifier);
    }

    /**
     * Retrieves an AppUser for login based on the provided identifier.
     *
     * @param identifier User identifier (email or phone)
     * @return AppUser object
     * @throws BusinessException if a user is not found
     */
    private AppUser findUserForLogin(String identifier) {
        return identifier.contains("@")
                ? appUserRepository.findByEmail(identifier)
                  .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "User not found with email: " + identifier))
                : appUserRepository.findByPhone(identifier)
                  .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "User not found with phone: " + identifier));
    }

    /**
     * Ensures that the user is registered via a local provider.
     *
     * @param user AppUser object
     * @throws BusinessException if the user is not registered via a local provider
     */
    private void ensureLocalProvider(AppUser user) {
        if (!userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), LOCAL)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Account registered via Google. Use Google login.");
        }
    }

    /**
     * Authenticates a user with the provided identifier and password.
     *
     * @param identifier User identifier (email or phone)
     * @param password   User password
     * @return AppUser object
     * @throws BusinessException if authentication fails
     */
    private AppUser authenticateUser(String identifier, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, password)
        );

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Authentication principal is null");
        }
        if (!(principal instanceof AppUserDetails appUserDetails)) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid principal type");
        }

        return appUserDetails.getAppUser();
    }

    /**
     * Ensures that the user's identifier is verified before login.
     *
     * @param user       AppUser object
     * @param identifier User identifier (email or phone)
     * @throws BusinessException if identifier verification fails
     */
    public void ensureIdentifierVerified(AppUser user, String identifier) {
        if (identifier == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Identifier cannot be null");
        }
        if (identifier.contains("@") && !user.isEmailVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "Email not verified. Please verify your email before logging in.");
        }
        if (!identifier.contains("@") && !user.isPhoneNumberVerified()) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED, "Phone number not verified. Please verify your phone before logging in.");
        }
    }

    /**
     * Records a successful login event.
     *
     * @param userId  User identifier
     * @param context Authentication request context
     */
    public void recordLoginSuccess(UUID userId, AuthRequestContext context) {
        meterRegistry.counter("auth.login.success").increment();
        auditEventLogger.log(SecurityAuditEvent.AUTH_LOGIN_SUCCESS, userId, context.deviceId(), context.ipAddress(), "success", null, context.requestId());
    }

    /**
     * Records a failed login event.
     *
     * @param userId  User identifier
     * @param context Authentication request context
     * @param message Failure message
     */
    private void recordLoginFailure(UUID userId, AuthRequestContext context, String message) {
        meterRegistry.counter("auth.login.failure").increment();
        auditEventLogger.log(SecurityAuditEvent.AUTH_LOGIN_FAILED, userId, context.deviceId(), context.ipAddress(), "failure", message, context.requestId());
    }

    /**
     * Writes authentication cookies to the HTTP response.
     *
     * @param response    HTTP response object
     * @param loginTokens Login tokens containing access and refresh tokens
     */
    public void writeAuthCookies(HttpServletResponse response, SessionTokenIssuer.LoginTokens loginTokens) {
        setAccessToken(response, loginTokens.accessToken());
        setRefreshToken(response, loginTokens.refreshToken());
    }

    /**
     * Represents the result of token refresh operations.
     *
     * @param userId       User identifier
     * @param accessToken  New access token
     * @param refreshToken New refresh token
     */
    private record TokenRefreshData(UUID userId, String accessToken, String refreshToken) {
        private SessionTokenIssuer.LoginTokens toLoginTokens() {
            return new SessionTokenIssuer.LoginTokens(accessToken, refreshToken);
        }
    }
}
