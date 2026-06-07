package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request record for user authentication/login operations.
 *
 * <p>This DTO is used to authenticate users and establish secure sessions.
 * It contains the user credentials needed to validate identity and issue JWT tokens.
 * Supports both email and phone number authentication methods for flexibility.</p>

 * <p><strong>Usage:</strong></p>
 * <p>Used in {@code AuthenticationController.loginUser()} endpoint at {@code /api/v1/auth/identifier/login}.
 * The service validates credentials against stored hashed passwords and issues secure HTTP-only
 * cookies containing access and refresh tokens for session management.</p>
 *
 * <p><strong>Lifecycle Note:</strong></p>
 * On successful primary credential verification, auth may either complete immediately (session
 * cookies issued) or return an MFA challenge continuation payload depending on account policy.
 * The {@code password} field is security-sensitive and must never be logged.
 *
 * @param identifier The user's email address or phone number for authentication.
 * @param password The user's password for authentication. Must be provided and non-blank.
 * @param deviceId The client-generated device id to bind the session to a trusted context.
 */
@Schema(name = "AppUserLoginRequest", description = "Request payload for local login using an email address or phone number plus password.")
public record AppUserLoginRequest(
        @NotBlank
        @Schema(description = "Email or phone used for login.", example = "intern.candidate@example.com")
        String identifier,

        @NotBlank(message = "password cannot be blank")
        @Schema(description = "Password created during sign-up.", example = "Intern@123")
        String password,

        @NotBlank
        @Schema(description = "Unique device fingerprint for session tracking.", example = "web-chrome-macos")
        String deviceId

) {
//todo max input size
}
