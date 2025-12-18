package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for handling phone number verification requests with OTP (One-Time Password).
 * This record encapsulates the phone number and the OTP code for verification purposes.
 *
 * @param phone The phone number to be verified. Must be a valid 10-digit number and cannot be blank.
 * @param otp        The one-time password for verification. Must be exactly 6 characters long.
 *
 * @see org.miniProjectTwo.DragonOfNorth.model.OtpToken The associated entity that this DTO maps to
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