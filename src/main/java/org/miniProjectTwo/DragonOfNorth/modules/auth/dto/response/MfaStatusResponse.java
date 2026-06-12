package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response indicating whether MFA is enabled for the user.
 */
@Schema(name = "MfaStatusResponse", description = "Response indicating whether MFA is enabled for the user.")
public record MfaStatusResponse(
        @Schema(description = "Indicates whether MFA is enabled for the user.")
        boolean mfaEnabled) {
}
