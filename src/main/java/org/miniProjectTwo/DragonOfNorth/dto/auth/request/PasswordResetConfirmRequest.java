package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

public record PasswordResetConfirmRequest(
        @NotBlank String identifier,
        @NotNull IdentifierType identifierType,

        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "Invalid OTP")
        String otp,
        @Size(min = 8, max = 50, message = "password length must be between 8 and 50")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                message = "password must include uppercase, lowercase, number, and special character"
        )
        @NotBlank
        String newPassword
) {
}
