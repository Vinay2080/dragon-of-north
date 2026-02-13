package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.services.otp.OtpServiceImpl;

/**
 * Request DTO for verifying OTP codes delivered via SMS.
 * <p>
 * Validates OTP against stored tokens and updates phone verification status. Purpose-based
 * verification ensures proper authentication flow completion. Failed attempts increment
 * security counters and may trigger rate limiting or account locks.
 *
 * @see OtpServiceImpl for verification logic
 * @see AppUser for status updates
 */

public record PhoneVerifyRequest(
        @Pattern(message = "invalid phone number", regexp = "[6-9]\\d{9}$")
        @Size(message = "invalid phone number size", min = 10, max = 14)
        @NotBlank(message = "this field cannot be empty")
        String phone,
        @NotBlank(message = "otp cannot be blank")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "invalid OTP"
        )
        String otp,
        @NotNull(message = "OTP purpose is required")
        OtpPurpose otpPurpose) {
}