package org.miniProjectTwo.DragonOfNorth.dto.OAuth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record OAuthLoginRequest(
        @NotBlank(message = "ID token is required")
        @Size(min = 100, max = 2000, message = "Id token length must be between 100 and 2000 characters")
        String idToken,

        @NotBlank(message = "Device Id is required")
        @Size(max = 255, message = "Device ID must not exceed 255 characters")
        String deviceId,

        @Size(max = 255, message = "Expected identifier must not exceed 255 characters")
        String expectedIdentifier
) {
}
