package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One-time response shown immediately after MFA enablement succeeds.
 * <p>
 * Recovery codes are sensitive break-glass credentials and must be stored by the end user because
 * plaintext values are not returned again.
 */
@Schema(name = "MfaSetupConfirmResponse", description = "Response returned after successfully enabling MFA. Contains recovery codes shown once to the user.")
public record MfaSetupConfirmResponse(
        @Schema(description = "One-time recovery codes. Store securely; only hashed versions are persisted.", example = "[\"ABCD-EFGH\",\"IJKL-MNOP\"]")
        String[] backupCodes
) {
}
