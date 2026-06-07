package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Device-scoped request payload shared by refresh, logout, account deletion, MFA setup, and session
 * revocation operations.
 * <p>
 * Accepts both {@code deviceId} and {@code device_id} for backward compatibility while preserving a
 * single canonical internal field.
 * @param deviceId The client-generated device identifier to scope the operation to a specific session/device context.
 */
@Schema(name = "DeviceIdRequest", description = "Request payload containing the client device identifier for session-scoped operations.")
public record DeviceIdRequest(
        @NotBlank
        @JsonProperty("deviceId")
        @JsonAlias("device_id")
        @Schema(description = "Client-generated device id used to bind refresh/session actions.", example = "web-chrome-macos")
        String deviceId) {
}
