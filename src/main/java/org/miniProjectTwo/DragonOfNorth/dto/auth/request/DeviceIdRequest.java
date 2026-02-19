package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for {@link org.miniProjectTwo.DragonOfNorth.model.Session}
 */
public record DeviceIdRequest(@NotBlank String deviceId) {
}