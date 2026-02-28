package org.miniProjectTwo.DragonOfNorth.services.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.dto.OAuth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.JwtServices;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.OAuthService;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.SessionService;
import org.miniProjectTwo.DragonOfNorth.services.GoogleTokenVerifierService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final GoogleTokenVerifierService tokenVerifierService;
    private final JwtServices jwtServices;
    private final SessionService sessionService;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final AuthCommonServiceImpl authCommonServiceImpl;

    @Override
    @Transactional
    public void authenticatedWithGoogle(String idToken, String deviceId, HttpServletRequest httpRequest, HttpServletResponse response) {
        OAuthUserInfo userInfo = tokenVerifierService.verifyToken(idToken);

        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        String userAgent = httpRequest.getHeader("User-Agent");

        AppUser appUser = findOrCreateUser(userInfo);
        updateLoginInfo(appUser);

        String accessToken = jwtServices.generateAccessToken(appUser.getId(), appUser.getRoles());
        String refreshToken = jwtServices.generateRefreshToken(appUser.getId());

        authCommonServiceImpl.setAccessToken(response, accessToken);
        authCommonServiceImpl.setRefreshToken(response, refreshToken);

        sessionService.createSession(appUser, refreshToken, ipAddress, deviceId, userAgent);
    }

    private AppUser findOrCreateUser(OAuthUserInfo userInfo) {
        Optional<AppUser> existingByProviderId = appUserRepository.findByProviderId(userInfo.sub());
        if (existingByProviderId.isPresent()) {
            return existingByProviderId.get();
        }
        Optional<Provider> existingProvider = appUserRepository.findProviderByEmail(userInfo.email());
        if (existingProvider.isPresent()) {
            Provider provider = existingProvider.get();

            if (provider == Provider.LOCAL) {
                // Need to fetch the full user to update it
                Optional<AppUser> existingByEmail = appUserRepository.findByEmail(userInfo.email());
                if (existingByEmail.isPresent()) {
                    AppUser appUser = existingByEmail.get();

                    appUser.setProvider(Provider.GOOGLE);
                    appUser.setProviderId(userInfo.sub());
                    appUser.setEmailVerified(true);
                    return appUser;
                }//todo throw exception
            } else {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email already associated with another OAuth provider");
            }
        }
        return createNewUserWithRetry(userInfo);
    }

    private void updateLoginInfo(AppUser user) {
        user.setLastLoginAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        // Dirty checking will save within transaction
    }

    private AppUser createNewUserWithRetry(OAuthUserInfo userInfo) {
        try {
            AppUser newUser = new AppUser();
            newUser.setEmail(userInfo.email());
            newUser.setProvider(Provider.GOOGLE);
            newUser.setProviderId(userInfo.sub());
            newUser.setEmailVerified(true);
            newUser.setPassword(null);
            newUser.setAppUserStatus(AppUserStatus.VERIFIED);
            newUser.setFailedLoginAttempts(0);
            newUser.setAccountLocked(false);

            // Assign default USER role
            Role userRole = roleRepository.findByRoleName(RoleName.USER)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "USER role not found"));
            newUser.setRoles(Set.of(userRole));

            return appUserRepository.save(newUser);

        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition during user creation, refetching: {}", userInfo.sub());
            // Handle race condition - another thread created by the user
            return appUserRepository.findByProviderId(userInfo.sub())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_CREATION_FAILED,
                            "Failed to create user"));
        }
    }

}
