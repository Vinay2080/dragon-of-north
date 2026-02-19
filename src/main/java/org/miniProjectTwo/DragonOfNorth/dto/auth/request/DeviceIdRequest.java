package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload carrying device identifier used for session-bound operations.
 */
public record DeviceIdRequest(@NotBlank(message = "deviceId cannot be blank") String deviceId) {
}
