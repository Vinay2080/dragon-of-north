package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;

/**
 * Data Transfer Object (DTO) for handling email-based OTP (One-Time Password) generation requests.
 * This record encapsulates the email address to which the OTP will be sent.
 *
 * @param email The email address of the user requesting OTP. Must be a valid email format and cannot be blank.
 *              The email is validated against a standard email pattern.
 *
 * @see AppUser The associated entity that this DTO maps to
 */


public record EmailOtpRequest(
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "enter a valid email")
        @NotBlank(message = "Email cannot be empty")
        String email)
{

}

