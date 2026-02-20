package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request record for user authentication/login operations.
 *
 * <p>This DTO is used to authenticate users and establish secure sessions.
 * It contains the user credentials needed to validate identity and issue JWT tokens.
 * Supports both email and phone number authentication methods for flexibility.</p>
 *
 * <p><strong>Input Components:</strong></p>
 * <ul>
 *   <li>{@code identifier} - The user's email address or phone number</li>
 *   <li>{@code password} - The user's password for authentication</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <p>Used in {@code AuthenticationController.loginUser()} endpoint at {@code /api/v1/auth/identifier/login}.
 * The service validates credentials against stored hashed passwords and issues secure HTTP-only
 * cookies containing access and refresh tokens for session management.</p>
 *
 * <p><strong>Output Flow:</strong></p>
 * <p>When authentication succeeds, returns a success message via {@code ApiResponse} wrapper
 * and sets secure cookies. Access tokens are short-lived while refresh tokens enable
 * session continuation without re-authentication.</p>
 */
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

}
