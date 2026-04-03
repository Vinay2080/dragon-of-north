package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

@Schema(name = "PasswordResetRequestOtpRequest", description = "Request payload for starting the password reset OTP flow.")
public record PasswordResetRequestOtpRequest(
        @JsonAlias({"email", "phone"})
        @NotBlank
        @Schema(description = "Email address or phone number for the account that needs a reset OTP.", example = "intern.candidate@example.com")
        String identifier,
        @JsonAlias("identifier_type")
        @NotNull
        @Schema(description = "Identifier type for the reset request.", allowableValues = {"EMAIL", "PHONE"}, example = "EMAIL")
        IdentifierType identifierType
) {
}
