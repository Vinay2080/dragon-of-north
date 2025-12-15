package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for {@link org.miniProjectTwo.DragonOfNorth.model.OtpToken}
 */
public record EmailVerifyRequest(
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "enter a valid email")
        @NotBlank(message = "phone number cannot be blank")
        String email,
        @NotBlank(message = "phone number cannot be blank")
        @Size(min = 6, max = 6)
        String otp) {

}