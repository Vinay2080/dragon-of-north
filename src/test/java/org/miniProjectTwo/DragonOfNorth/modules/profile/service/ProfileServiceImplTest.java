package org.miniProjectTwo.DragonOfNorth.modules.profile.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
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

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;


    @InjectMocks
    private ProfileServiceImpl profileService;

    @AfterEach
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
    void syncGoogleAvatar_shouldNotUpdate_whenAlreadyGoogleSynced() {
        UUID userId = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setAvatarSource(AvatarSource.GOOGLE);
        profile.setAvatarUrl("https://google.example/existing.png");
        profile.setAvatarExternalUrl("https://google.example/existing.png");

        OAuthUserInfo userInfo = OAuthUserInfo.builder().picture("https://google.example/new.png").build();
        when(profileRepository.findByAppUserId(userId)).thenReturn(Optional.of(profile));

        profileService.syncGoogleAvatar(userId, userInfo);

        assertEquals("https://google.example/existing.png", profile.getAvatarUrl());
        assertEquals("https://google.example/existing.png", profile.getAvatarExternalUrl());
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

    @Test
    void updateProfileImage_shouldUploadAndPersistMetadata() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        appUser.setId(userId);

        Profile profile = new Profile();
        profile.setAvatarPublicId("old_public_id");
        profile.setAvatarUrl("https://old.example/avatar.png");
        profile.setAvatarSource(AvatarSource.USER_DEFINED);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "content".getBytes()
        );

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));
        when(profileRepository.findByAppUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("old_public_id"), anyMap())).thenReturn(Map.of());
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/profile_images/new.png",
                "public_id", "profile_images/new"
        ));

        Profile result = profileService.updateProfileImage(userId, file);

        assertNull(result.getAvatarExternalUrl());
        assertEquals("profile_images/new", result.getAvatarPublicId());
        assertEquals("https://res.cloudinary.com/demo/image/upload/v1/profile_images/new.png", result.getAvatarUrl());
        assertEquals(AvatarSource.USER_DEFINED, result.getAvatarSource());
        verify(uploader).destroy(eq("old_public_id"), anyMap());
        verify(uploader).upload(eq("content".getBytes()), argThat(options -> "profile_images".equals(options.get("folder"))));
        verify(userStateValidator).validate(appUser, UserLifecycleOperation.PROFILE_UPDATE);
    }

    @Test
    void updateProfileImage_shouldRejectInvalidFileType() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                "content".getBytes()
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> profileService.updateProfileImage(userId, file));

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        verifyNoInteractions(appUserRepository);
    }

    @Test
    void updateProfileImage_shouldRejectSuspiciousFilename() {
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bad\"name.png",
                "image/png",
                "content".getBytes()
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> profileService.updateProfileImage(userId, file));

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        assertEquals("Invalid file format: unsupported filename", exception.getMessage());
        verifyNoInteractions(appUserRepository);
    }

    @Test
    void updateProfileImage_shouldReturnSpecificError_whenCloudinaryRejectsImageFormat() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        appUser.setId(userId);

        Profile profile = new Profile();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "content".getBytes()
        );

        when(appUserRepository.findById(userId)).thenReturn(Optional.of(appUser));
        when(profileRepository.findByAppUserId(userId)).thenReturn(Optional.of(profile));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new RuntimeException("Invalid image file"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> profileService.updateProfileImage(userId, file));

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        assertEquals("Invalid image format. Allowed formats: image/jpeg, image/png, image/webp", exception.getMessage());
    }

    @Test
    void deleteProfileImage_shouldRemoveStoredImageAndResetAvatar() throws Exception {
        UUID userId = UUID.randomUUID();
        Profile profile = new Profile();
        profile.setAvatarPublicId("profile_images/old");
        profile.setAvatarUrl("https://res.cloudinary.com/demo/image/upload/v1/profile_images/old.png");
        profile.setAvatarSource(AvatarSource.USER_DEFINED);

        when(profileRepository.findByAppUserId(userId)).thenReturn(Optional.of(profile));
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("profile_images/old"), anyMap())).thenReturn(Map.of());

        profileService.deleteProfileImage(userId);

        assertNull(profile.getAvatarPublicId());
        assertNull(profile.getAvatarUrl());
        assertNull(profile.getAvatarExternalUrl());
        assertEquals(AvatarSource.NONE, profile.getAvatarSource());
        verify(uploader).destroy(eq("profile_images/old"), anyMap());
    }
}

