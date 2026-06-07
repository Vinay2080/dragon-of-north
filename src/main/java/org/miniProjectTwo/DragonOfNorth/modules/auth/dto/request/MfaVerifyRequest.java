package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;

/**
 * Request DTO for MFA verification.
 * <p>
 * Used by {@code /auth/mfa/verify} to submit an MFA code for an in-progress MFA challenge. The challenge id is required to look up the pending MFA context, and the provider type is required to route the verification logic to the appropriate MFA provider. The device id is required to bind the MFA verification to a session context for security and auditing purposes.
 *
 * @param challengeId
 * @param providerType
 * @param code
 * @param deviceId
 */
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
