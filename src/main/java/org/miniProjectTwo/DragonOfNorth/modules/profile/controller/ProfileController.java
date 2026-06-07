package org.miniProjectTwo.DragonOfNorth.modules.profile.controller;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.profile.api.ProfileApi;
import org.miniProjectTwo.DragonOfNorth.modules.profile.dto.UpdateProfileRequest;
import org.miniProjectTwo.DragonOfNorth.modules.profile.dto.response.GetProfileResponse;
import org.miniProjectTwo.DragonOfNorth.modules.profile.dto.response.ProfileImageResponse;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.UNAUTHORIZED;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.GOOGLE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

/**
 * Profile controller for authenticated user profile read/update operations.
 * <p>
 * This controller resolves the caller identity from Spring Security principals and delegates
 * profile persistence to {@code ProfileService}. Keeping this boundary thin simplifies future
 * security principal refactors and provider-resolution changes.
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController implements ProfileApi {

    private final ProfileService profileService;
    private final UserAuthProviderRepository userAuthProviderRepository;

    /**
     * Updates the authenticated user's profile information, including bio, avatar URL, display name, and username. The method accepts an UpdateProfileRequest DTO containing the new profile details, resolves the user's identity from the Spring Security context, and delegates the update operation to the ProfileService. Upon successful update, it returns the updated profile information along with the primary authentication provider wrapped in a GetProfileResponse DTO within a successful ApiResponse envelope.
     *
     * @param request An UpdateProfileRequest DTO containing the new profile details to be updated. Must not be null.
     * @return An ApiResponse containing a GetProfileResponse DTO with the updated profile information and primary authentication provider.
     * @throws BusinessException If the user is not authenticated, or if there is an issue resolving the user's identity or authentication provider.
     */
    @Override
    @PatchMapping
    public ApiResponse<GetProfileResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        Profile profile = profileService.updateProfile(
                request.bio(),
                request.avatarUrl(),
                request.displayName(),
                request.username()
        );
        return ApiResponse.success(toResponse(profile, resolveAuthProvider(resolveCurrentUserId())));
    }

    /**
     * Uploads a new profile image for the authenticated user. The method accepts a multipart file upload, resolves the user's identity from the Spring Security context, and delegates the image processing and profile update to the ProfileService. Upon successful update, it returns the new avatar URL and source wrapped in a ProfileImageResponse DTO within a successful ApiResponse envelope.
     *
     * @param file The multipart file containing the new profile image to be uploaded. Must not be null.
     * @return An ApiResponse containing a ProfileImageResponse DTO with the updated avatar URL and source.
     * @throws BusinessException If the user is not authenticated, or if there is an issue, resolving the user's identity.
     */
    @Override
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileImageResponse> uploadProfileImage(
            @RequestPart("file") MultipartFile file) {
        UUID userId = resolveCurrentUserId();
        Profile profile = profileService.updateProfileImage(userId, file);
        return ApiResponse.success(toImageResponse(profile));
    }

    private ProfileImageResponse toImageResponse(Profile profile) {
        return new ProfileImageResponse(
                profile.getAvatarUrl(),
                profile.getAvatarSource()
        );
    }

    /**
     * Retrieves the current user's profile information, including username, display name, bio, avatar URL, avatar source, and primary authentication provider. The method resolves the user's identity from the Spring Security context, fetches the associated profile from the ProfileService, determines the primary authentication provider using the UserAuthProviderRepository, and returns the profile information wrapped in a GetProfileResponse DTO within a successful ApiResponse envelope.
     *
     * @return An ApiResponse containing a GetProfileResponse DTO with the user's profile information and primary authentication provider.
     * @throws BusinessException If the user is not authenticated, or if there is an issue resolving the user's identity or authentication provider.
     */
    @Override
    @GetMapping
    public ApiResponse<GetProfileResponse> getProfile() {
        Profile profile = profileService.getProfile();
        UUID userId = resolveCurrentUserId();
        Provider authProvider = resolveAuthProvider(userId);

        return ApiResponse.success(toResponse(profile, authProvider));
    }

    /**
     * Converts a Profile entity and an authentication provider into a GetProfileResponse DTO.
     *
     * @param profile      The profile entity to convert.
     * @param authProvider The authentication provider associated with the profile.
     * @return A GetProfileResponse DTO containing the profile information and auth provider.
     */
    private GetProfileResponse toResponse(Profile profile, Provider authProvider) {
        return new GetProfileResponse(
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                profile.getAvatarSource(),
                authProvider
        );
    }

    /**
     * Resolves the primary authentication provider for the given user ID. This method checks the user's linked auth providers in a defined order (e.g., LOCAL first, then GOOGLE) and returns the first match. If no providers are found, it returns null, indicating an unknown or unsupported provider.
     *
     * @param userId The ID of the user for whom to resolve the auth provider.
     * @return The resolved Provider enum value representing the user's primary auth provider, or null if no provider is found.
     */
    private Provider resolveAuthProvider(UUID userId) {
        if (userAuthProviderRepository.existsByUserIdAndProvider(userId, LOCAL)) {
            return LOCAL;
        }

        if (userAuthProviderRepository.existsByUserIdAndProvider(userId, GOOGLE)) {
            return GOOGLE;
        }

        return null;
    }

    /**
     * Resolves the current authenticated user's ID from the Spring Security context. This method checks various possible principal types (AppUserDetails, AppUser, SecurityPrincipal, raw UUID string) to extract the user ID. If no valid user ID can be resolved, it throws a BusinessException with an UNAUTHORIZED error code.
     *
     * @return The UUID of the currently authenticated user.
     * @throws BusinessException If the user is not authenticated, or if the authentication principal is of an unsupported type.
     */
    private UUID resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BusinessException(UNAUTHORIZED, "User must be authenticated to view profile");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AppUserDetails appUserDetails && appUserDetails.getAppUser().getId() != null) {
            return appUserDetails.getAppUser().getId();
        }

        if (principal instanceof AppUser appUser && appUser.getId() != null) {
            return appUser.getId();
        }

        if (principal instanceof SecurityPrincipal securityPrincipal && securityPrincipal.userId() != null) {
            return securityPrincipal.userId();
        }

        if (principal instanceof UUID userId) {
            return userId;
        }

        if (principal instanceof String raw && !raw.isBlank() && !"anonymousUser".equals(raw)) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                // fall through to unauthorized
            }
        }

        throw new BusinessException(UNAUTHORIZED, "Unsupported authentication principal");
    }
}
