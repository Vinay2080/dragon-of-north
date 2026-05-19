package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "MfaSetupConfirmResponse", description = "Response returned after successfully enabling MFA. Contains recovery codes shown once to the user.")
public record MfaSetupConfirmResponse(
        @Schema(description = "One-time recovery codes. Store securely; only hashed versions are persisted.", example = "[\"ABCD-EFGH\",\"IJKL-MNOP\"]")
        String[] backupCodes
) {
}
