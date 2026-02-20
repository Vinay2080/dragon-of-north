package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;

/**
 * Request DTO for verifying OTP codes delivered via email.
 */
public record EmailVerifyRequest(
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "enter a valid email")
        @NotBlank(message = "email cannot be blank")
        @Schema(description = "Email identifier used to request OTP.", example = "intern.candidate@example.com")
        String email,
        @NotBlank(message = "otp cannot be blank")
        @Pattern(regexp = "^\\d{6}$", message = "invalid OTP")
        @Schema(description = "Six digit OTP code received by user.", example = "123456")
        String otp,
        @NotNull(message = "OTP purpose cannot be null")
        @Schema(description = "Purpose must match the original OTP request.", allowableValues = {"SIGNUP", "LOGIN", "PASSWORD_RESET", "TWO_FACTOR_AUTH"}, example = "SIGNUP")
        OtpPurpose otpPurpose) {

}
