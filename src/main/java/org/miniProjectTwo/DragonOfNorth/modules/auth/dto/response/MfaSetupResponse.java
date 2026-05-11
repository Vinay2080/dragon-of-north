package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MfaSetupResponse", description = "Response returned when starting MFA setup. Contains the TOTP secret and a QR code image payload.")
public record MfaSetupResponse(
        @Schema(description = "Base32-encoded secret to register in an authenticator app.", example = "JBSWY3DPEHPK3PXP")
        String mfaSecret,

        @Schema(description = "QR code image (data URL, base64 PNG) that encodes the TOTP provisioning URI.")
        String mfaQrCode
) {
}
