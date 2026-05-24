package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Completes passwordless authentication using an emailed token and device fingerprint.
 * <p>
 * This DTO enters at passwordless verification endpoint after email-link delivery. Token and device
 * id are both security-critical: token proves possession of inbox link; device id binds issued
 * session state for refresh/logout controls. Successful verification may still branch into MFA.
 */
@Schema(name = "VerifyPasswordlessLoginDto", description = "Request payload for completing passwordless login.")
public record VerifyPasswordlessLoginDto(
        @NotBlank(message = "token is required")
        @Schema(description = "Passwordless login token received from the email link.")
        String token,

        @NotBlank(message = "device_id is required")
        @JsonAlias("device_id")
        @Schema(description = "Client-generated device id used to bind the created session.", example = "web-chrome-macos")
        String deviceId
) {
}
