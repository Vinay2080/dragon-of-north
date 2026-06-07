package org.miniProjectTwo.DragonOfNorth.modules.profile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.AvatarSource;

/**
 * Response DTO for profile image upload operations.
 *
 * <p>This record encapsulates the response data returned after a user uploads a profile image, including the public URL of the avatar and its source.</p>
 */
@Schema(name = "ProfileImageResponse", description = "Response returned after uploading a profile image.")
public record ProfileImageResponse(
        @Schema(description = "Public avatar image URL.", example = "https://cdn.dragonofnorth.dev/avatars/arya.png", nullable = true)
        String avatarUrl,
        @Schema(description = "Source of the avatar image.", example = "USER_DEFINED")
        AvatarSource avatarSource
) {
}
