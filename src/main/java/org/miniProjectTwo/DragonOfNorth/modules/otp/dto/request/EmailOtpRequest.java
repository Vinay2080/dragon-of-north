package org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpPurpose;

/**
 * Request DTO for generating OTP codes via email delivery.
 */
@Schema(name = "EmailOtpRequest", description = "Request payload for sending an OTP to an email address.")
public record EmailOtpRequest(
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "enter a valid email")
        @NotBlank(message = "Email cannot be empty")
        @Schema(description = "Target email address where OTP should be delivered.", example = "intern.candidate@example.com")
        String email,
        @NotNull(message = "OTP purpose cannot be null")
        @Schema(description = "Business flow for OTP issuance.", allowableValues = {"SIGNUP", "LOGIN", "PASSWORD_RESET", "TWO_FACTOR_AUTH"}, example = "SIGNUP")
        OtpPurpose otpPurpose
) {

}
