package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

public record MfaSetupConfirmResponse(
        String[] backupCodes
) {
}
