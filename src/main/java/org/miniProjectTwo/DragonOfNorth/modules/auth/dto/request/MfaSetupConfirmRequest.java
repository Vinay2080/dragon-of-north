package org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record MfaSetupConfirmRequest(
        @NotNull
        @Length(min = 6, max = 6, message = "OTP code must be exactly 6 digits")
        String code,
        String deviceId
) {
}
