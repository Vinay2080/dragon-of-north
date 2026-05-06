package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

//dto for password less login via link requesting email
@Schema(name = "RequestPasswordlessLoginDto", description = "Request payload for initiating passwordless login via email link.")
public record RequestPasswordlessLoginDto(
        @NotBlank
        @Email
        @Schema(description = "Email address to receive the passwordless login link.", example = "user@example.com")
        String email
) {
}
