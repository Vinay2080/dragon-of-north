package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.impl.otp.OtpService;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;

/**
 * Request DTO for verifying OTP codes delivered via email.
 * Validates OTP against stored tokens and updates user verification status. Purpose-based
 * verification ensures proper authentication flow completion. Failed attempts increment
 * security counters and may trigger rate limiting or account locks.
 *
 * @see OtpService for verification logic
 * @see AppUser for status updates
 */

public record EmailVerifyRequest(
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "enter a valid email")
        @NotBlank(message = "phone number cannot be blank")
        String email,
        @NotBlank(message = "phone number cannot be blank")
        @NotBlank(message = "otp cannot be blank")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "invalid OTP"
        )
        String otp,
        @NotNull(message = "OTP purpose cannot be null")
        OtpPurpose otpPurpose) {

}