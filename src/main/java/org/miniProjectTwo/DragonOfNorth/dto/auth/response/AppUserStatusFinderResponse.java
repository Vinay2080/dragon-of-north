package org.miniProjectTwo.DragonOfNorth.dto.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;

/**
 * Response record for user status information and authentication availability.
 */
public record AppUserStatusFinderResponse(
        @NotNull
        @Schema(description = "Current lifecycle status of the user account.", allowableValues = {"NOT_EXIST", "CREATED", "VERIFIED", "DELETED"}, example = "CREATED")
        AppUserStatus appUserStatus) {
}
