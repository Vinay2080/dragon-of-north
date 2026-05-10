package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

public record MfaSetupResponse(
        String mfaSecret,
        String mfaQrCode
) {
}
