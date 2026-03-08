package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;

public record PasswordResetConfirmRequest(
        @JsonAlias({"email", "phone"})
        @NotBlank String identifier,
        @JsonAlias("identifier_type")
        @NotNull IdentifierType identifierType,

        @NotBlank
        @Pattern(regexp = "^\\d{6}$", message = "Invalid OTP")
        String otp,
        @Size(min = 8, max = 50, message = "password length must be between 8 and 50")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                message = "password must include uppercase, lowercase, number, and special character"
        )
        @JsonAlias("new_password")
        @NotBlank
        String newPassword
) {
}
