package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.miniProjectTwo.DragonOfNorth.config.OtpConfig.SnsConfig;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.services.otp.OtpServiceImpl;

/**
 * Request DTO for generating OTP codes via email delivery.
 * Triggers OTP generation and email delivery through SNS service. Purpose-based routing
 * ensures appropriate verification flows (registration, login, recovery). Email format validation
 * prevents invalid delivery attempts and reduces service costs.
 *
 * @see OtpServiceImpl for generation logic
 * @see SnsConfig for email delivery configuration
 */


public record EmailOtpRequest(
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "enter a valid email")
        @NotBlank(message = "Email cannot be empty")
        String email,
        @NotNull(message = "OTP purpose cannot be null")
        OtpPurpose otpPurpose
) {

}

