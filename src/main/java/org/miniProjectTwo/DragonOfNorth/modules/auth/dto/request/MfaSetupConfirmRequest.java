package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

/**
 * Confirms TOTP enrollment by submitting the current authenticator code for the in-progress setup.
 * <p>
 * Security expectations: code must be exactly six digits and device id must be present so enablement
 * can be bound to a trusted session context.
 */
@Schema(name = "MfaSetupConfirmRequest", description = "Request payload for confirming TOTP/MFA setup using an authenticator app code.")
public record MfaSetupConfirmRequest(
        @NotNull
        @Length(min = 6, max = 6, message = "OTP code must be exactly 6 digits")
        @JsonProperty("code")
        @JsonAlias("otp")
        @Schema(description = "6-digit TOTP code from the authenticator app.", example = "123456")
        String code,

        @NotBlank(message = "device_id is required")
        @JsonProperty("deviceId")
        @JsonAlias("device_id")
        @Schema(description = "Client-generated device id used to bind the MFA enablement to a session.", example = "web-chrome-macos")
        String deviceId
) {
}
