package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for {@link org.miniProjectTwo.DragonOfNorth.model.OtpToken}
 */
public record PhoneVerifyRequest(
        @Pattern(message = "invalid phone number", regexp = "^\\+91[6-9]\\d{9}$")
        @Size(message = "invalid phone number size", min = 10, max = 12)
        @NotBlank(message = "this field cannot be empty")
        String phone,
        @Size(message = "invalid OTP size", min = 6, max = 6)
        @NotBlank(message = "this field cannot be blank")
        String otp) {
}