package org.miniProjectTwo.DragonOfNorth.shared.dto.oauth;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(name = "OAuthLoginRequest", description = "Request payload for Google OAuth login and Google OAuth sign-up.")
public record OAuthLoginRequest(
        @NotBlank(message = "ID token is required")
        @Size(min = 100, max = 2000, message = "Id token length must be between 100 and 2000 characters")
        @JsonAlias({"id_token", "credential"})
        @Schema(description = "Google ID token returned by the Google Sign-In client flow.", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...")
        String idToken,

        @NotBlank(message = "Device Id is required")
        @Size(max = 255, message = "Device ID must not exceed 255 characters")
        @JsonAlias("device_id")
        @Schema(description = "Stable client-generated device identifier used for session tracking.", example = "web-chrome-macos")
        String deviceId,

        @Size(max = 255, message = "Expected identifier must not exceed 255 characters")
        @JsonAlias("expected_identifier")
        @Schema(description = "Optional email address expected from the Google account to prevent account mismatch.", example = "intern.candidate@example.com", nullable = true)
        String expectedIdentifier
) {
}
