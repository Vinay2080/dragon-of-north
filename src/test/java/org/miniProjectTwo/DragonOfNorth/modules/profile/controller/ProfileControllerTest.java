package org.miniProjectTwo.DragonOfNorth.modules.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.AvatarSource;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.shared.exception.ApplicationExceptionHandler;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ProfileController profileController;

    @Mock
    private ProfileService profileService;

    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
                .setControllerAdvice(new ApplicationExceptionHandler())
                .build();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Profile buildProfile() {
        Profile profile = new Profile();
        profile.setUsername("dragon_rider");
        profile.setDisplayName("Dragon Rider");
        profile.setBio("Riding dragons since 300 AC");
        profile.setAvatarUrl("https://cdn.example/avatar.png");
        profile.setAvatarSource(AvatarSource.USER_DEFINED);
        return profile;
    }

    private void setAuthContext(Object principal) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ── GET /api/v1/profile ──────────────────────────────────────────────────

    @Test
    void getProfile_shouldReturnProfile_whenPrincipalIsUuid() throws Exception {
        UUID userId = UUID.randomUUID();
        setAuthContext(userId);

        Profile profile = buildProfile();
        when(profileService.getProfile()).thenReturn(profile);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(true);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.username").value("dragon_rider"))
                .andExpect(jsonPath("$.data.authProvider").value("LOCAL"));
    }

    @Test
    void getProfile_shouldReturnGoogleProvider_whenUserHasGoogleAuth() throws Exception {
        UUID userId = UUID.randomUUID();
        setAuthContext(userId);

        Profile profile = buildProfile();
        when(profileService.getProfile()).thenReturn(profile);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(false);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.GOOGLE)).thenReturn(true);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authProvider").value("GOOGLE"));
    }

    @Test
    void getProfile_shouldReturnProfile_whenPrincipalIsAppUserDetails() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser appUser = new AppUser();
        appUser.setId(userId);
        AppUserDetails details = new AppUserDetails(appUser);
        setAuthContext(details);

        Profile profile = buildProfile();
        when(profileService.getProfile()).thenReturn(profile);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(true);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));
    }

    @Test
    void getProfile_shouldReturnProfile_whenPrincipalIsUuidString() throws Exception {
        UUID userId = UUID.randomUUID();
        setAuthContext(userId.toString());

        Profile profile = buildProfile();
        when(profileService.getProfile()).thenReturn(profile);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(false);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.GOOGLE)).thenReturn(false);

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authProvider").isEmpty());
    }

    @Test
    void getProfile_shouldReturn401_whenNoPrincipal() throws Exception {
        SecurityContextHolder.clearContext();

        when(profileService.getProfile())
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED, "User must be authenticated to view profile"));

        mockMvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ── PATCH /api/v1/profile ────────────────────────────────────────────────

    @Test
    void updateProfile_shouldReturnUpdatedProfile_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        setAuthContext(userId);

        Profile profile = buildProfile();
        profile.setDisplayName("Updated Name");

        when(profileService.updateProfile(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(profile);
        when(userAuthProviderRepository.existsByUserIdAndProvider(userId, Provider.LOCAL)).thenReturn(true);

        String payload = """
                {
                    "displayName": "Updated Name",
                    "avatarUrl": "https://cdn.example/new.png",
                    "bio": "New bio",
                    "username": "new_username"
                }
                """;

        mockMvc.perform(patch("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.displayName").value("Updated Name"));
    }

    @Test
    void updateProfile_shouldReturn409_whenUsernameAlreadyTaken() throws Exception {
        UUID userId = UUID.randomUUID();
        setAuthContext(userId);

        when(profileService.updateProfile(any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.USERNAME_ALREADY_TAKEN));

        String payload = """
                {
                    "username": "taken_name"
                }
                """;

        mockMvc.perform(patch("/api/v1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value(ErrorCode.USERNAME_ALREADY_TAKEN.getCode()));
    }

    @Test
    void uploadProfileImage_shouldReturnAvatarMetadata_whenUploadSucceeds() throws Exception {
        UUID userId = UUID.randomUUID();
        setAuthContext(userId);

        Profile profile = buildProfile();
        profile.setAvatarUrl("https://res.cloudinary.com/demo/image/upload/v1/profile_images/new.png");
        profile.setAvatarSource(AvatarSource.USER_DEFINED);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "content".getBytes()
        );

        when(profileService.updateProfileImage(eq(userId), any(MultipartFile.class))).thenReturn(profile);

        mockMvc.perform(multipart("/api/v1/profile/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.avatarUrl").value("https://res.cloudinary.com/demo/image/upload/v1/profile_images/new.png"))
                .andExpect(jsonPath("$.data.avatarSource").value("USER_DEFINED"));
    }
}
