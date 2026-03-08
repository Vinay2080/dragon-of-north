package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;

import java.util.List;

public record AppUserStatusFinderResponse(
        @Schema(description = "Whether an account exists for the identifier.", example = "true")
        boolean exists,

        @NotNull
        @Schema(description = "Linked providers for the account.", example = "[\"LOCAL\",\"GOOGLE\"]")
        List<Provider> providers,

        @Schema(description = "Whether the account email is verified.", example = "true")
        boolean emailVerified,

        @Schema(description = "Current account status.", allowableValues = {"ACTIVE", "LOCKED", "DELETED"}, example = "ACTIVE")
        AppUserStatus appUserStatus
) {
    public static AppUserStatusFinderResponse notFound() {
        return new AppUserStatusFinderResponse(false, List.of(), false, null);
    }
}
