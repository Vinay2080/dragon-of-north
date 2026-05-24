package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

/**
 * Input for pre-auth account discovery checks.
 * <p>
 * Used before login/sign-up branches to determine whether an identifier already exists and which
 * flow the client should render. Validation rejects blank identifiers and missing identifier type.
 */
@Schema(name = "AppUserStatusFinderRequest", description = "Request payload used to inspect whether an account exists for an email address or phone number.")
public record AppUserStatusFinderRequest(
        @NotBlank(message = "identifier cannot be blank")
        @Schema(description = "Identifier to search. Can be email or phone number.", example = "intern.candidate@example.com")
        String identifier,

        @NotNull(message = "Identifier type is required")
        @Schema(description = "Identifier type for routing the auth flow.", allowableValues = {"EMAIL", "PHONE"}, example = "EMAIL")
        IdentifierType identifierType) {
}
