package org.miniProjectTwo.DragonOfNorth.modules.profile.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.profile.repo.ProfileRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void createProfile_shouldUseOauthInfo_whenOauthUserProvided() {
        AppUser appUser = new AppUser();
        appUser.setEmail("user@example.com");

        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .email("user@example.com")
                .name("John Smith")
                .picture("https://cdn.example/avatar.png")
                .build();

        when(profileRepository.findProfileByAppUser(appUser)).thenReturn(false);
        when(profileRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);

        profileService.createProfile(appUser, userInfo);

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
        AppUser appUser = new AppUser();
        appUser.setEmail("mail.user@example.com");

        when(profileRepository.findProfileByAppUser(appUser)).thenReturn(false);
        when(profileRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);

        profileService.createProfile(appUser, null);

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(captor.capture());
        Profile saved = captor.getValue();

        assertEquals("mail.user@example.com", saved.getDisplayName());
        assertNotNull(saved.getUsername());
        assertTrue(saved.getUsername().startsWith("mailuserexamplecom_"));
    }

    @Test
    void createProfile_shouldThrow_whenProfileAlreadyExistsForUser() {
        AppUser appUser = new AppUser();
        appUser.setEmail("already@exists.com");

        when(profileRepository.findProfileByAppUser(appUser)).thenReturn(true);

        assertThrows(BusinessException.class, () -> profileService.createProfile(appUser, null));

        verify(profileRepository, never()).save(any(Profile.class));
    }
}


