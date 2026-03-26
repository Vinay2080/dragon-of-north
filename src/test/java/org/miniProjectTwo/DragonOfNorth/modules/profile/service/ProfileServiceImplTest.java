package org.miniProjectTwo.DragonOfNorth.modules.profile.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.profile.repo.ProfileRepository;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.impl.ProfileServiceImpl;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
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

        Profile profile = new Profile();
        profile.setUsername("old_name");
        profile.setBio("old_bio");
        profile.setAvatarUrl("old_avatar");
        profile.setDisplayName("old_display");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
        SecurityContextHolder.setContext(context);

        when(profileRepository.findByAppUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.existsByUsernameIgnoreCase("new_name")).thenReturn(false);

        profileService.updateProfile("new_bio", "new_avatar", "new_display", "new_name");

        assertEquals("new_name", profile.getUsername());
        assertEquals("new_bio", profile.getBio());
        assertEquals("new_avatar", profile.getAvatarUrl());
        assertEquals("new_display", profile.getDisplayName());
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

