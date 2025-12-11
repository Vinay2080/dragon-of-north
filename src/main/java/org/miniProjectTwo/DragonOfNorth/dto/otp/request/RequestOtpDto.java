package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RequestOtpDto(
        @NotBlank String identifier,
        @NotBlank @Pattern(regexp = "EMAIL|PHONE") String type
) {
}
