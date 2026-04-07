package org.miniProjectTwo.DragonOfNorth.modules.profile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.AvatarSource;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;

@Schema(name = "GetProfileResponse", description = "Profile avatar details returned for the authenticated user.")
public record GetProfileResponse(
        @Schema(description = "Unique public username for the profile.", example = "arya_north")
        String username,
        @Schema(description = "Display name shown to other users.", example = "Arya Stark")
        String displayName,
        @Schema(description = "Short biography shown on the profile.", example = "Explorer, archer, and dragon rider in training.", nullable = true)
        String bio,
        @Schema(description = "Public avatar image URL.", example = "https://cdn.dragonofnorth.dev/avatars/arya.png", nullable = true)
        String avatarUrl,
        @Schema(description = "Source of the avatar image.", example = "USER_DEFINED")
        AvatarSource avatarSource,
        @Schema(description = "Authentication provider currently associated with the profile.", example = "LOCAL", nullable = true)
        Provider authProvider
) {
}
