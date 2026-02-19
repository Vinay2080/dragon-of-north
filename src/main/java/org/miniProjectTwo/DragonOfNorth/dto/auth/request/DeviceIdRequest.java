package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO for {@link org.miniProjectTwo.DragonOfNorth.model.Session}
 */
public record DeviceIdRequest(@NotNull String deviceId) {
}