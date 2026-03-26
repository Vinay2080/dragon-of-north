package org.miniProjectTwo.DragonOfNorth.modules.profile.controller;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.profile.dto.UpdateProfileRequest;
import org.miniProjectTwo.DragonOfNorth.modules.profile.dto.response.GetProfileResponse;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PatchMapping
    public ApiResponse<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        profileService.updateProfile(request.bio(), request.avatarUrl(), request.displayName(), request.username());
        return ApiResponse.successMessage("profile updated");
    }

    @GetMapping
    public ApiResponse<GetProfileResponse> getProfile() {
        Profile profile = profileService.getProfile();
        return ApiResponse.success(new GetProfileResponse(
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getBio(),
                profile.getAvatarUrl()
        ));
    }
}
