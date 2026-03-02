package org.miniProjectTwo.DragonOfNorth.services.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.OAuth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.JwtServices;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.SessionService;
import org.miniProjectTwo.DragonOfNorth.services.GoogleTokenVerifierService;
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

        oAuthService.authenticatedWithGoogle("token", "device-1", null, request, response);

        verify(appUserRepository).save(any(AppUser.class));
        verify(userAuthProviderRepository).save(any(UserAuthProvider.class));
        verify(sessionService).createSession(eq(newUser), eq("refresh"), any(), eq("device-1"), any());
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

        oAuthService.authenticatedWithGoogle("token", "device-2", "existing@example.com", request, response);

        ArgumentCaptor<UserAuthProvider> authProviderCaptor = ArgumentCaptor.forClass(UserAuthProvider.class);
        verify(userAuthProviderRepository).save(authProviderCaptor.capture());

        UserAuthProvider provider = authProviderCaptor.getValue();
        assertEquals(Provider.GOOGLE, provider.getProvider());
        assertEquals("google-sub-2", provider.getProviderId());
        assertEquals(existingUser, provider.getUser());
        assertTrue(existingUser.isEmailVerified());
    }
}
