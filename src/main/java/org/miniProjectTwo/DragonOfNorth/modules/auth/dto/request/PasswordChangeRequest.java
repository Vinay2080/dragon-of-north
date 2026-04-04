package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "PasswordChangeRequest", description = "Request payload for changing the authenticated user's password.")
public record PasswordChangeRequest(
        @NotBlank
        @JsonProperty("oldPassword")
        @JsonAlias({"old_password", "currentPassword", "current_password"})
        @Schema(description = "Current password for the authenticated account.", example = "Intern@123")
        String oldPassword,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "Password must be at least 8 characters with letters and numbers"
        )
        @JsonProperty("newPassword")
        @JsonAlias({"new_password"})
        @Schema(description = "New password to store after validation.", example = "Updated@123")
        String newPassword
) {
}
