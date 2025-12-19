package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;

/**
 * Data Transfer Object (DTO) for handling email verification requests with OTP (One-Time Password).
 * This record encapsulates the email address and the OTP code for verification purposes.
 *
 * @param email The email address to be verified. Must be a valid email format and cannot be blank.
 * @param otp   The one-time password for verification. Must be exactly 6 characters long.
 * @see org.miniProjectTwo.DragonOfNorth.model.OtpToken The associated entity that this DTO maps to
 */

public record EmailVerifyRequest(
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "enter a valid email")
        @NotBlank(message = "phone number cannot be blank")
        String email,
        @NotBlank(message = "phone number cannot be blank")
        @Size(min = 6, max = 6)
        String otp,
        OtpPurpose otpPurpose) {

}