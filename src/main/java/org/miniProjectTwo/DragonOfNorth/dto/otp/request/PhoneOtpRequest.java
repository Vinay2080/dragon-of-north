package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.config.OtpConfig.SnsConfig;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.impl.otp.OtpService;

/**
 * Request DTO for generating OTP codes via SMS delivery.
 * Triggers OTP generation and SMS delivery through SNS service. Phone validation
 * ensures the proper Indian mobile format (6-9 prefix, 10 digits). Purpose-based routing
 * controls verification flows and prevents SMS spam to invalid numbers.
 * @see OtpService for generation logic
 * @see SnsConfig for SMS delivery configuration
 */

public record PhoneOtpRequest(
        @Size(message = "invalid phone number size", min = 10, max = 14)
        @Pattern(message = "invalid phone number", regexp = "[6-9]\\d{9}$")
        @NotBlank(message = "phone number cannot be blank")
        String phone,
        @NotNull(message = "OTP purpose cannot be null")
        OtpPurpose otpPurpose) {

}