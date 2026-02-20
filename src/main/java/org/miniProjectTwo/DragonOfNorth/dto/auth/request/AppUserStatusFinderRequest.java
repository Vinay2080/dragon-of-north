package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

/**
 * Request record for checking user registration status and existence.
 */
public record AppUserStatusFinderRequest(
        @NotBlank(message = "identifier cannot be blank")
        @Schema(description = "Identifier to search. Can be email or phone number.", example = "intern.candidate@example.com")
        String identifier,

        @NotNull(message = "Identifier type is required")
        @Schema(description = "Identifier type for routing the auth flow.", allowableValues = {"EMAIL", "PHONE"}, example = "EMAIL")
        IdentifierType identifierType) {
}
