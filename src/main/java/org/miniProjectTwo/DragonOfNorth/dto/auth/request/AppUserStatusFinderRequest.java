package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

/**
 * DTO for {@link org.miniProjectTwo.DragonOfNorth.model.AppUser}
 */
public record AppUserStatusFinderRequest(
        @NotBlank(message = "identifier cannot be blank")
        String identifier,

        @NotNull(message = "Identifier type is required")
        IdentifierType identifierType) {
}