package org.miniProjectTwo.DragonOfNorth.modules.profile.controller;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.profile.api.ProfileApi;
import org.miniProjectTwo.DragonOfNorth.modules.profile.dto.UpdateProfileRequest;
import org.miniProjectTwo.DragonOfNorth.modules.profile.dto.response.GetProfileResponse;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode.UNAUTHORIZED;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.GOOGLE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController implements ProfileApi {

    private final ProfileService profileService;
    private final UserAuthProviderRepository userAuthProviderRepository;

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

    @Override
    @GetMapping
    public ApiResponse<GetProfileResponse> getProfile() {
        Profile profile = profileService.getProfile();
        UUID userId = resolveCurrentUserId();
        Provider authProvider = resolveAuthProvider(userId);

        return ApiResponse.success(toResponse(profile, authProvider));
    }

    private GetProfileResponse toResponse(Profile profile, Provider authProvider) {
        return new GetProfileResponse(
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl(),
                authProvider
        );
    }

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

    private Provider resolveAuthProvider(UUID userId) {
        if (userAuthProviderRepository.existsByUserIdAndProvider(userId, LOCAL)) {
            return LOCAL;
        }

        if (userAuthProviderRepository.existsByUserIdAndProvider(userId, GOOGLE)) {
            return GOOGLE;
        }

        return null;
    }
}
