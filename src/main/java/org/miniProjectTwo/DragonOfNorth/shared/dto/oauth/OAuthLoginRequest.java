package org.miniProjectTwo.DragonOfNorth.shared.dto.oauth;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record OAuthLoginRequest(
        @NotBlank(message = "ID token is required")
        @Size(min = 100, max = 2000, message = "Id token length must be between 100 and 2000 characters")
        @JsonAlias({"id_token", "credential"})
        String idToken,

        @NotBlank(message = "Device Id is required")
        @Size(max = 255, message = "Device ID must not exceed 255 characters")
        @JsonAlias("device_id")
        String deviceId,

        @Size(max = 255, message = "Expected identifier must not exceed 255 characters")
        @JsonAlias("expected_identifier")
        String expectedIdentifier
) {
}
