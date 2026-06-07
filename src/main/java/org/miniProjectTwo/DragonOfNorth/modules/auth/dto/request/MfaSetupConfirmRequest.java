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
 * Security expectations: code must be exactly six digits, and device id must be present so enablement
 * can be bound to a trusted session context.
 * <p>
 * Used by {@code /auth/mfa/setup/confirm} to finalize TOTP/MFA setup after the user has scanned the QR code and configured their authenticator app. The submitted code is verified against the expected TOTP value for the shared secret, and if valid, the MFA method is enabled for the user's account and associated with the provided device id for session binding. This step is crucial to ensure that the user has successfully configured their authenticator app and can generate valid codes before enabling MFA for their account.
 * @param code The 6-digit TOTP code from the user's authenticator app.
 * @param deviceId The client-generated device id to bind the MFA enablement to a session
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
