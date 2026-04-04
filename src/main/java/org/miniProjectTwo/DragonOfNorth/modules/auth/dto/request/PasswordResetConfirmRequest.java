package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

@Schema(name = "PasswordResetConfirmRequest", description = "Request payload for resetting a local account password with a verified OTP.")
public record PasswordResetConfirmRequest(
        @JsonAlias({"email", "phone"})
        @NotBlank
        @Schema(description = "Email address or phone number for the account being updated.", example = "intern.candidate@example.com")
        String identifier,
        @JsonAlias("identifier_type")
        @NotNull
        @Schema(description = "Identifier type used for password reset.", allowableValues = {"EMAIL", "PHONE"}, example = "EMAIL")
        IdentifierType identifierType,

        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "Invalid OTP")
        @Schema(description = "Six-digit OTP received through the password reset flow.", example = "123456")
        String otp,
        @Size(min = 8, max = 50, message = "password length must be between 8 and 50")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                message = "password must include uppercase, lowercase, number, and special character"
        )
        @JsonAlias("new_password")
        @NotBlank
        @Schema(description = "Replacement password that satisfies the platform password policy.", example = "Reset@123")
        String newPassword
) {
}
