package org.miniProjectTwo.DragonOfNorth.modules.profile.dto.response;

public record GetProfileResponse(
        String username,
        String displayName,
        String bio,
        String avatarUrl
) {
}
