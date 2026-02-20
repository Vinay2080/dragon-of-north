package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;

/**
 * Request DTO for verifying OTP codes delivered via SMS.
 */
public record PhoneVerifyRequest(
        @Pattern(message = "invalid phone number", regexp = "[6-9]\\d{9}$")
        @Size(message = "invalid phone number size", min = 10, max = 14)
        @NotBlank(message = "this field cannot be empty")
        @Schema(description = "Phone identifier used to request OTP.", example = "9876543210")
        String phone,
        @NotBlank(message = "otp cannot be blank")
        @Pattern(regexp = "^\\d{6}$", message = "invalid OTP")
        @Schema(description = "Six digit OTP code received by user.", example = "123456")
        String otp,
        @NotNull(message = "OTP purpose is required")
        @Schema(description = "Purpose must match the original OTP request.", allowableValues = {"SIGNUP", "LOGIN", "PASSWORD_RESET", "TWO_FACTOR_AUTH"}, example = "LOGIN")
        OtpPurpose otpPurpose) {
}
