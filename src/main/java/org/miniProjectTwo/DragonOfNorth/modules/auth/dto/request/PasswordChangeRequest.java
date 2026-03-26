package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank
        @JsonProperty("oldPassword")
        @JsonAlias({"old_password", "currentPassword", "current_password"})
        String oldPassword,
        @NotBlank
        @JsonProperty("newPassword")
        @JsonAlias({"new_password"})
        String newPassword
) {
}
