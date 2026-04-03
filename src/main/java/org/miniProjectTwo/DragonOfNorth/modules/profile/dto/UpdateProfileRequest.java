package org.miniProjectTwo.DragonOfNorth.modules.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UpdateProfileRequest", description = "Request payload for updating the authenticated user's profile. All fields are optional.")
public record UpdateProfileRequest(
        @Schema(description = "Display name shown in profile and account screens.", example = "Arya Stark", nullable = true)
        String displayName,
        @Schema(description = "Public avatar image URL.", example = "https://cdn.dragonofnorth.dev/avatars/arya.png", nullable = true)
        String avatarUrl,
        @Schema(description = "Short biography shown on the profile.", example = "Explorer, archer, and dragon rider in training.", nullable = true)
        String bio,
        @Schema(description = "Unique public username for the profile.", example = "arya_north", nullable = true)
        String username

) {
}
