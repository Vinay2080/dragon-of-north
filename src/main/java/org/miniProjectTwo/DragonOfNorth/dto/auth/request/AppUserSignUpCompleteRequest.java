package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

/**
 * Request record for completing the user sign-up process.
 */
public record AppUserSignUpCompleteRequest(
        @NotBlank
        @Schema(description = "Identifier that has completed OTP verification.", example = "intern.candidate@example.com")
        String identifier,
        @NotNull
        @Schema(description = "Type of identifier used during sign-up.", allowableValues = {"EMAIL", "PHONE"}, example = "EMAIL")
        IdentifierType identifierType) {
}
