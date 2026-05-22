package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.GoogleTokenVerifierService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.SessionTokenIssuer;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.SessionCreationSpec;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceImplTest {

    @Mock
    private GoogleTokenVerifierService tokenVerifierService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuthCommonServices authCommonServices;
    @Mock
    private SessionTokenIssuer sessionTokenIssuer;
    @Mock
    private ProfileService profileService;
    @Mock
    private AuditEventLogger auditEventLogger;
    @Mock
    private UserStateValidator userStateValidator;

    @InjectMocks
    private OAuthServiceImpl oAuthService;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Test
    void authenticatedWithGoogle_createsAccountWhenNoUserExists() {
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .sub("google-sub")
                .email("new@example.com")
                .build();

        Role role = new Role();
        role.setRoleName(RoleName.USER);

        AppUser newUser = new AppUser();
        newUser.setId(UUID.randomUUID());
        newUser.setEmail("new@example.com");
        newUser.setRoles(Set.of(role));

        when(tokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userAuthProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-sub")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmailForUpdate("new@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName(RoleName.USER)).thenReturn(Optional.of(role));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(newUser);
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getHeader("X-Request-Id")).thenReturn("req-1");
        when(sessionTokenIssuer.issueLoginSession(eq(newUser), any(SessionCreationSpec.class), any(), eq("device-1"), any()))
                .thenReturn(new SessionTokenIssuer.LoginTokens("access", "refresh"));
        doNothing().when(authCommonServices).setAccessToken(any(HttpServletResponse.class), anyString());
        doNothing().when(authCommonServices).setRefreshToken(any(HttpServletResponse.class), anyString());

        oAuthService.authenticatedWithGoogle("token", "device-1", null, request, response);

        verify(appUserRepository).save(any(AppUser.class));
        verify(userAuthProviderRepository).save(any(UserAuthProvider.class));
        verify(profileService).ensureProfileExists(newUser.getId(), userInfo);
        verify(profileService).syncGoogleAvatar(newUser.getId(), userInfo);
        verify(sessionTokenIssuer).issueLoginSession(eq(newUser), any(SessionCreationSpec.class), any(), eq("device-1"), any());
        verify(authCommonServices).setAccessToken(response, "access");
        verify(authCommonServices).setRefreshToken(response, "refresh");
        verify(auditEventLogger).log("auth.oauth.google.login", newUser.getId(), "device-1", "127.0.0.1", "success", null, "req-1");
    }

    @Test
    void authenticatedWithGoogle_linksGoogleProviderForExistingEmailUser() {
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .sub("google-sub-2")
                .email("existing@example.com")
                .build();

        Role role = new Role();
        role.setRoleName(RoleName.USER);

        AppUser existingUser = new AppUser();
        existingUser.setId(UUID.randomUUID());
        existingUser.setEmail("existing@example.com");
        existingUser.setRoles(Set.of(role));
        existingUser.setAppUserStatus(AppUserStatus.ACTIVE);

        when(tokenVerifierService.verifyToken("token")).thenReturn(userInfo);
        when(userAuthProviderRepository.findByProviderAndProviderId(Provider.GOOGLE, "google-sub-2")).thenReturn(Optional.empty());
        when(appUserRepository.findByEmailForUpdate("existing@example.com")).thenReturn(Optional.of(existingUser));
        when(userAuthProviderRepository.existsByUserIdAndProvider(existingUser.getId(), Provider.GOOGLE)).thenReturn(false);
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getHeader("X-Request-Id")).thenReturn("req-2");
        when(sessionTokenIssuer.issueLoginSession(eq(existingUser), any(SessionCreationSpec.class), any(), eq("device-2"), any()))
                .thenReturn(new SessionTokenIssuer.LoginTokens("access", "refresh"));
        doNothing().when(authCommonServices).setAccessToken(any(HttpServletResponse.class), anyString());
        doNothing().when(authCommonServices).setRefreshToken(any(HttpServletResponse.class), anyString());

        oAuthService.authenticatedWithGoogle("token", "device-2", "existing@example.com", request, response);

        ArgumentCaptor<UserAuthProvider> authProviderCaptor = ArgumentCaptor.forClass(UserAuthProvider.class);
        verify(userAuthProviderRepository).save(authProviderCaptor.capture());
        verify(userStateValidator, times(2)).validate(existingUser, UserLifecycleOperation.GOOGLE_LOGIN);
        verify(profileService, never()).ensureProfileExists(any(UUID.class), any());
        verify(profileService).syncGoogleAvatar(existingUser.getId(), userInfo);
        verify(authCommonServices).setAccessToken(response, "access");
        verify(authCommonServices).setRefreshToken(response, "refresh");
        verify(auditEventLogger).log("auth.oauth.google.login", existingUser.getId(), "device-2", "127.0.0.1", "success", null, "req-2");
    }
}
