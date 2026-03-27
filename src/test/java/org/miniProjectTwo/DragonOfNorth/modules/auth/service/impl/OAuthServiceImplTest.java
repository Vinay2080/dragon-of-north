package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.GoogleTokenVerifierService;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceImplTest {

    @Mock
    private GoogleTokenVerifierService tokenVerifierService;
    @Mock
    private JwtServices jwtServices;
    @Mock
    private SessionService sessionService;
    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private AuthCommonServiceImpl authCommonServiceImpl;
    @Mock
    private ProfileService profileService;
    @Mock
    private AuditEventLogger auditEventLogger;

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
        when(jwtServices.generateAccessToken(eq(newUser.getId()), anySet())).thenReturn("access");
        when(jwtServices.generateRefreshToken(newUser.getId())).thenReturn("refresh");
        doNothing().when(authCommonServiceImpl).setAccessToken(any(HttpServletResponse.class), anyString());
        doNothing().when(authCommonServiceImpl).setRefreshToken(any(HttpServletResponse.class), anyString());

        oAuthService.authenticatedWithGoogle("token", "device-1", null, request, response);

        verify(appUserRepository).save(any(AppUser.class));
        verify(userAuthProviderRepository).save(any(UserAuthProvider.class));
        verify(profileService).createProfile(newUser.getId(), userInfo);
        verify(profileService).syncGoogleAvatar(newUser.getId(), userInfo);
        verify(sessionService).createSession(eq(newUser), eq("refresh"), any(), eq("device-1"), any());
        verify(authCommonServiceImpl).setAccessToken(response, "access");
        verify(authCommonServiceImpl).setRefreshToken(response, "refresh");
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
        when(jwtServices.generateAccessToken(eq(existingUser.getId()), anySet())).thenReturn("access");
        when(jwtServices.generateRefreshToken(existingUser.getId())).thenReturn("refresh");
        doNothing().when(authCommonServiceImpl).setAccessToken(any(HttpServletResponse.class), anyString());
        doNothing().when(authCommonServiceImpl).setRefreshToken(any(HttpServletResponse.class), anyString());

        oAuthService.authenticatedWithGoogle("token", "device-2", "existing@example.com", request, response);

        ArgumentCaptor<UserAuthProvider> authProviderCaptor = ArgumentCaptor.forClass(UserAuthProvider.class);
        verify(userAuthProviderRepository).save(authProviderCaptor.capture());
        verify(profileService, never()).createProfile(any(UUID.class), any());
        verify(profileService).syncGoogleAvatar(existingUser.getId(), userInfo);
        verify(authCommonServiceImpl).setAccessToken(response, "access");
        verify(authCommonServiceImpl).setRefreshToken(response, "refresh");

        UserAuthProvider provider = authProviderCaptor.getValue();
        assertEquals(Provider.GOOGLE, provider.getProvider());
        assertEquals("google-sub-2", provider.getProviderId());
        assertEquals(existingUser, provider.getUser());
        assertTrue(existingUser.isEmailVerified());
    }
}
