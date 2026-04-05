package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.GoogleTokenVerifierService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.OAuthService;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
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
 * Handles Google OAuth login/signup and account linking flows.
 */
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final GoogleTokenVerifierService tokenVerifierService;
    private final JwtServices jwtServices;
    private final SessionService sessionService;
    private final AppUserRepository appUserRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final RoleRepository roleRepository;
    private final AuthCommonServiceImpl authCommonServiceImpl;
    private final ProfileService profileService;
    private final AuditEventLogger auditEventLogger;
    private final UserStateValidator userStateValidator;

    /**
     * Authenticates a user with Google and establishes an application session.
     */
    @Override
    @Transactional
    public void authenticatedWithGoogle(String idToken, String deviceId, String expectedIdentifier, HttpServletRequest httpRequest, HttpServletResponse response) {
        executeGoogleFlow(
                "auth.oauth.google.login",
                idToken,
                deviceId,
                expectedIdentifier,
                httpRequest,
                response,
                this::findOrCreateUserForGoogleAuth
        );
    }

    /**
     * Creates or links an account with Google signup flow and establishes a session.
     */
    @Override
    @Transactional
    public void signupWithGoogle(String idToken, String deviceId, String expectedIdentifier, HttpServletRequest httpRequest, HttpServletResponse response) {
        executeGoogleFlow(
                "auth.oauth.google.signup",
                idToken,
                deviceId,
                expectedIdentifier,
                httpRequest,
                response,
                this::findOrCreateUserForSignup
        );
    }

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

    private void executeGoogleFlow(String eventName,
                                   String idToken,
                                   String deviceId,
                                   String expectedIdentifier,
                                   HttpServletRequest httpRequest,
                                   HttpServletResponse response,
                                   GoogleUserResolver userResolver) {
        RequestMetadata metadata = extractRequestMetadata(httpRequest);
        UUID auditUserId = null;
        try {
            OAuthUserInfo userInfo = verifyGoogleIdentity(idToken, expectedIdentifier);
            AppUser appUser = userResolver.resolve(userInfo);
            synchronizeGoogleProfile(appUser, userInfo);
            auditUserId = appUser.getId();
            finalizeAuthentication(appUser, deviceId, httpRequest, response);
            recordOauthSuccess(eventName, auditUserId, deviceId, metadata);
        } catch (BusinessException exception) {
            recordOauthFailure(eventName, auditUserId, deviceId, metadata, exception);
            throw exception;
        }
    }

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

    private RequestMetadata extractRequestMetadata(HttpServletRequest httpRequest) {
        return new RequestMetadata(
                httpRequest.getHeader("X-Forwarded-For"),
                httpRequest.getHeader("X-Request-Id")
        );
    }

    private OAuthUserInfo verifyGoogleIdentity(String idToken, String expectedIdentifier) {
        OAuthUserInfo userInfo = tokenVerifierService.verifyToken(idToken);
        validateExpectedIdentifier(userInfo, expectedIdentifier);
        return userInfo;
    }

    private void synchronizeGoogleProfile(AppUser appUser, OAuthUserInfo userInfo) {
        profileService.syncGoogleAvatar(appUser.getId(), userInfo);
    }


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

    private void finalizeAuthentication(AppUser appUser, String deviceId, HttpServletRequest httpRequest, HttpServletResponse response) {
        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        String userAgent = httpRequest.getHeader("User-Agent");

        updateLoginInfo(appUser);

        String accessToken = jwtServices.generateAccessToken(appUser.getId(), appUser.getRoles());
        String refreshToken = jwtServices.generateRefreshToken(appUser.getId());

        authCommonServiceImpl.setAccessToken(response, accessToken);
        authCommonServiceImpl.setRefreshToken(response, refreshToken);

        sessionService.createSession(appUser, refreshToken, ipAddress, deviceId, userAgent);
    }

    private void recordOauthSuccess(String eventName, UUID userId, String deviceId, RequestMetadata metadata) {
        auditEventLogger.log(eventName, userId, deviceId, metadata.ipAddress(), "success", null, metadata.requestId());
    }

    private void recordOauthFailure(String eventName, UUID userId, String deviceId, RequestMetadata metadata, BusinessException exception) {
        auditEventLogger.log(eventName, userId, deviceId, metadata.ipAddress(), "failure", exception.getMessage(), metadata.requestId());
    }

    private void updateLoginInfo(AppUser user) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
    }

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

    private AppUser markGoogleIdentityVerified(AppUser user) {
        user.setEmailVerified(true);
        return user;
    }

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

    @FunctionalInterface
    private interface GoogleUserResolver {
        AppUser resolve(OAuthUserInfo userInfo);
    }

    private record RequestMetadata(String ipAddress, String requestId) {
    }
}
