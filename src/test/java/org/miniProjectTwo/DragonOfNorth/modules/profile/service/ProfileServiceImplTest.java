package org.miniProjectTwo.DragonOfNorth.modules.profile.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.AvatarSource;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.profile.repo.ProfileRepository;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.impl.ProfileServiceImpl;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private UserStateValidator userStateValidator;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProfile_shouldUseOauthInfo_whenOauthUserProvided() {
        UUID userId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        appUser.setId(userId);
        appUser.setEmail("user@example.com");

        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .email("user@example.com")
                .name("John Smith")
                .picture("https://cdn.example/avatar.png")
                .build();

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));
        when(profileRepository.existsByAppUserId(userId)).thenReturn(false);
        when(profileRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);

        profileService.createProfile(userId, userInfo);

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(captor.capture());
        Profile saved = captor.getValue();

        assertEquals(appUser, saved.getAppUser());
        assertEquals("John Smith", saved.getDisplayName());
        assertEquals("https://cdn.example/avatar.png", saved.getAvatarUrl());
        assertEquals(AvatarSource.GOOGLE, saved.getAvatarSource());
        assertEquals("https://cdn.example/avatar.png", saved.getAvatarExternalUrl());
        assertNotNull(saved.getUsername());
        assertTrue(saved.getUsername().startsWith("johnsmith_"));
    }

    @Test
    void createProfile_shouldUseEmailFallback_whenOauthInfoIsNull() {
        UUID userId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        appUser.setId(userId);
        appUser.setEmail("mail.user@example.com");

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));
        when(profileRepository.existsByAppUserId(userId)).thenReturn(false);
        when(profileRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);

        profileService.createProfile(userId, null);

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(captor.capture());
        Profile saved = captor.getValue();

        assertEquals("mail.user@example.com", saved.getDisplayName());
        assertEquals(AvatarSource.NONE, saved.getAvatarSource());
        assertNotNull(saved.getUsername());
        assertTrue(saved.getUsername().startsWith("mailuserexamplecom_"));
    }

    @Test
    void createProfile_shouldThrow_whenProfileAlreadyExistsForUser() {
        UUID userId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        appUser.setId(userId);
        appUser.setEmail("already@exists.com");

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));
        when(profileRepository.existsByAppUserId(userId)).thenReturn(true);

        assertThrows(BusinessException.class, () -> profileService.createProfile(userId, null));

        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void updateProfile_shouldUpdateFields_whenPrincipalIsUuid() {
        UUID userId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        appUser.setId(userId);

        Profile profile = new Profile();
        profile.setUsername("old_name");
        profile.setBio("old_bio");
        profile.setAvatarUrl("old_avatar");
        profile.setAvatarSource(AvatarSource.NONE);
        profile.setDisplayName("old_display");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
        SecurityContextHolder.setContext(context);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));
        when(profileRepository.findByAppUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.existsByUsernameIgnoreCase("new_name")).thenReturn(false);

        profileService.updateProfile("new_bio", "new_avatar", "new_display", "new_name");

        assertEquals("new_name", profile.getUsername());
        assertEquals("new_bio", profile.getBio());
        assertEquals("new_avatar", profile.getAvatarUrl());
        assertEquals(AvatarSource.USER_DEFINED, profile.getAvatarSource());
        assertNull(profile.getAvatarExternalUrl());
        assertEquals("new_display", profile.getDisplayName());
        verify(userStateValidator).validate(appUser, UserLifecycleOperation.PROFILE_UPDATE);
    }

    @Test
    void updateProfile_shouldResetAvatarSource_whenAvatarCleared() {
        UUID userId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        appUser.setId(userId);

        Profile profile = new Profile();
        profile.setAvatarUrl("https://google.example/avatar.png");
        profile.setAvatarSource(AvatarSource.USER_DEFINED);
        profile.setAvatarExternalUrl("https://google.example/avatar.png");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
        SecurityContextHolder.setContext(context);

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));
        when(profileRepository.findByAppUserId(userId)).thenReturn(Optional.of(profile));

        profileService.updateProfile(null, "   ", null, null);

        assertNull(profile.getAvatarUrl());
        assertEquals(AvatarSource.NONE, profile.getAvatarSource());
        assertNull(profile.getAvatarExternalUrl());
        verify(userStateValidator).validate(appUser, UserLifecycleOperation.PROFILE_UPDATE);
    }

    @Test
    void syncGoogleAvatar_shouldSkip_whenAvatarIsUserDefined() {
        UUID userId = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setAvatarSource(AvatarSource.USER_DEFINED);
        profile.setAvatarUrl("https://custom.example/avatar.png");

        OAuthUserInfo userInfo = OAuthUserInfo.builder().picture("https://google.example/new.png").build();
        when(profileRepository.findByAppUserId(userId)).thenReturn(Optional.of(profile));

        profileService.syncGoogleAvatar(userId, userInfo);

        assertEquals("https://custom.example/avatar.png", profile.getAvatarUrl());
        assertEquals(AvatarSource.USER_DEFINED, profile.getAvatarSource());
        assertNull(profile.getAvatarExternalUrl());
    }

    @Test
    void syncGoogleAvatar_shouldUpdate_whenAvatarIsMissing() {
        UUID userId = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setAvatarSource(AvatarSource.NONE);

        OAuthUserInfo userInfo = OAuthUserInfo.builder().picture("https://google.example/new.png").build();
        when(profileRepository.findByAppUserId(userId)).thenReturn(Optional.of(profile));

        profileService.syncGoogleAvatar(userId, userInfo);

        assertEquals("https://google.example/new.png", profile.getAvatarUrl());
        assertEquals("https://google.example/new.png", profile.getAvatarExternalUrl());
        assertEquals(AvatarSource.GOOGLE, profile.getAvatarSource());
    }

    @Test
    void updateProfile_shouldThrowUnauthorized_whenPrincipalTypeIsUnsupported() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(new Object(), null, List.of()));
        SecurityContextHolder.setContext(context);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> profileService.updateProfile("bio", "avatar", "display", "username"));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verifyNoInteractions(appUserRepository);
    }

    @Test
    void updateProfile_shouldThrowUnauthorized_whenAuthenticationMissing() {
        SecurityContextHolder.clearContext();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> profileService.updateProfile("bio", "avatar", "display", "username"));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        verifyNoInteractions(appUserRepository);
    }
}

