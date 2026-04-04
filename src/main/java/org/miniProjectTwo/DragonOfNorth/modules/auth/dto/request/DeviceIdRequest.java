package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body containing the device identifier for session-scoped operations.
 */
@Schema(name = "DeviceIdRequest", description = "Request payload containing the client device identifier for session-scoped operations.")
public record DeviceIdRequest(
        @NotBlank
        @Schema(description = "Client-generated device id used to bind refresh/session actions.", example = "web-chrome-macos")
        String deviceId) {
}
