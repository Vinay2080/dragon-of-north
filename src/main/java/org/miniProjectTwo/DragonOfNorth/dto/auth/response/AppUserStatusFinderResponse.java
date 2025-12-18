package org.miniProjectTwo.DragonOfNorth.dto.auth.response;

import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;

/**
 * DTO for {@link org.miniProjectTwo.DragonOfNorth.model.AppUser}
 */
public record AppUserStatusFinderResponse(@NotNull AppUserStatus appUserStatus) {
}