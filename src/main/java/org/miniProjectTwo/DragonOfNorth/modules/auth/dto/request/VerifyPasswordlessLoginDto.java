package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

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
