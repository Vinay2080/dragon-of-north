package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasswordChangeRequest(
        @NotBlank
        String oldPassword,
        @NotNull
        String newPassword
) {
}
