package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

public record MfaVerifyRequest(
        @NotBlank
        @JsonProperty("challenge_id")
        String challengeId,

        @NotNull
        @JsonProperty("provider_type")
        ProviderType providerType,

        @NotBlank
        String code,

        @NotBlank
        @JsonProperty("device_id")
        String deviceId
) {
}
