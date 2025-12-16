package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for handling phone-number-based OTP (One-Time Password) generation requests.
 * This record encapsulates the phone number to which the OTP will be sent.
 *
 * @param phone The phone number of the user requesting OTP. Must be a valid 10-digit number
 *              and cannot be blank. The phone number is validated against a standard pattern.
 * @see org.miniProjectTwo.DragonOfNorth.model.AppUser The associated entity that this DTO maps to
 */

public record PhoneOtpRequest(
        @Size(message = "invalid phone number size", min = 10, max = 12)
        @Pattern(message = "invalid phone number", regexp = "^\\+91[6-9]\\d{9}$")
        @NotBlank(message = "phone number cannot be blank")
        String phone) {

}