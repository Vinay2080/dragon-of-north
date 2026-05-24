package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


/**
 * Starts passwordless login by requesting a signed one-time link for the supplied email.
 * <p>
 * Used by public login flows; servers should keep response wording non-enumerating for security.
 */
@Schema(name = "RequestPasswordlessLoginDto", description = "Request payload for initiating passwordless login via email link.")
public record RequestPasswordlessLoginDto(
        @NotBlank
        @Email
        @Schema(description = "Email address to receive the passwordless login link.", example = "user@example.com")
        String email
) {
}
