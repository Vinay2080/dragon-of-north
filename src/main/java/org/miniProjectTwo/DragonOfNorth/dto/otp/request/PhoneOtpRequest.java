package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;

/**
 * Request DTO for generating OTP codes via SMS delivery.
 */
public record PhoneOtpRequest(
        @Size(message = "invalid phone number size", min = 10, max = 14)
        @Pattern(message = "invalid phone number", regexp = "[6-9]\\d{9}$")
        @NotBlank(message = "phone number cannot be blank")
        @Schema(description = "Phone number used for OTP delivery.", example = "9876543210")
        String phone,
        @NotNull(message = "OTP purpose cannot be null")
        @Schema(description = "Business flow for OTP issuance.", allowableValues = {"SIGNUP", "LOGIN", "PASSWORD_RESET", "TWO_FACTOR_AUTH"}, example = "LOGIN")
        OtpPurpose otpPurpose) {

}
