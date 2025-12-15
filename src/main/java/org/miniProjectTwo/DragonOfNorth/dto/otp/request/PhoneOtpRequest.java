package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for {@link org.miniProjectTwo.DragonOfNorth.model.OtpToken}
 */
public record PhoneOtpRequest(
        @Pattern(message = "invalid phone number", regexp = "^\\+91[6-9]\\d{9}$")
        @NotBlank(message = "phone number cannot be blank")
        String phone) {

}