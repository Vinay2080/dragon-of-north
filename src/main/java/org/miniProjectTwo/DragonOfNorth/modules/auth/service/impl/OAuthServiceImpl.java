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
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
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

    /**
     * Authenticates a user with Google and establishes an application session.
     */
    @Override
    @Transactional
    public void authenticatedWithGoogle(String idToken, String deviceId, String expectedIdentifier, HttpServletRequest httpRequest, HttpServletResponse response) {
        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        String requestId = httpRequest.getHeader("X-Request-Id");
        UUID auditUserId = null;
        try {
            OAuthUserInfo userInfo = tokenVerifierService.verifyToken(idToken);
            validateExpectedIdentifier(userInfo, expectedIdentifier);
            AppUser appUser = findOrCreateUserForGoogleAuth(userInfo);
            auditUserId = appUser.getId();
            finalizeAuthentication(appUser, deviceId, httpRequest, response);
            auditEventLogger.log("auth.oauth.google.login", auditUserId, deviceId, ipAddress, "success", null, requestId);
        } catch (BusinessException exception) {
            auditEventLogger.log("auth.oauth.google.login", auditUserId, deviceId, ipAddress, "failure", exception.getMessage(), requestId);
            throw exception;
        }
    }

    /**
     * Creates or links an account with Google signup flow and establishes a session.
     */
    @Override
    @Transactional
    public void signupWithGoogle(String idToken, String deviceId, String expectedIdentifier, HttpServletRequest httpRequest, HttpServletResponse response) {
        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        String requestId = httpRequest.getHeader("X-Request-Id");
        UUID auditUserId = null;
        try {
            OAuthUserInfo userInfo = tokenVerifierService.verifyToken(idToken);
            validateExpectedIdentifier(userInfo, expectedIdentifier);
            AppUser appUser = findOrCreateUserForSignup(userInfo);
            auditUserId = appUser.getId();
            finalizeAuthentication(appUser, deviceId, httpRequest, response);
            auditEventLogger.log("auth.oauth.google.signup", auditUserId, deviceId, ipAddress, "success", null, requestId);
        } catch (BusinessException exception) {
            auditEventLogger.log("auth.oauth.google.signup", auditUserId, deviceId, ipAddress, "failure", exception.getMessage(), requestId);
            throw exception;
        }
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

    private AppUser findOrCreateUserForGoogleAuth(OAuthUserInfo userInfo) {
        Optional<UserAuthProvider> existingByProviderId = userAuthProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, userInfo.sub());
        if (existingByProviderId.isPresent()) {
            return existingByProviderId.get().getUser();
        }

        Optional<AppUser> existingByEmail = appUserRepository.findByEmailForUpdate(userInfo.email());
        if (existingByEmail.isPresent()) {
            AppUser user = existingByEmail.get();
            linkGoogleProvider(user, userInfo.sub());
            user.setEmailVerified(true);
            return user;
        }

        return createNewUserWithRetry(userInfo);
    }

    private AppUser findOrCreateUserForSignup(OAuthUserInfo userInfo) {
        Optional<UserAuthProvider> existingByProviderId = userAuthProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, userInfo.sub());
        if (existingByProviderId.isPresent()) {
            return existingByProviderId.get().getUser();
        }

        Optional<AppUser> existingByEmail = appUserRepository.findByEmailForUpdate(userInfo.email());
        if (existingByEmail.isPresent()) {
            AppUser user = existingByEmail.get();
            if (userAuthProviderRepository.existsByUserIdAndProvider(user.getId(), Provider.GOOGLE)) {
                throw new BusinessException(ErrorCode.INVALID_OAUTH_TOKEN, "Google account mismatch. Please login again.");
            }
            throw new BusinessException(ErrorCode.OAUTH_LINK_CONFIRMATION_REQUIRED,
                    "Account already exists. Login with password before linking Google.");
        }

        return createNewUserWithRetry(userInfo);
    }

    private void updateLoginInfo(AppUser user) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
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
            profileService.createProfile(savedUser, userInfo);
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
            return existingUser;
        }
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
}
