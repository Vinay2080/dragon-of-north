package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MfaSetupConfirmResponse", description = "Response returned after successfully enabling MFA. Contains one-time recovery (backup) codes.")
public record MfaSetupConfirmResponse(
        @Schema(description = "One-time recovery codes to regain access if the authenticator device is unavailable.", example = "[\"ABCD-EFGH\",\"IJKL-MNOP\"]")
        String[] backupCodes
) {
}
