package org.miniProjectTwo.DragonOfNorth.modules.profile.dto;

public record UpdateProfileRequest(
        String displayName,
        String avatarUrl,
        String bio,
        String username

) {
}
