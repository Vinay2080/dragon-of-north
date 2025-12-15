package org.miniProjectTwo.DragonOfNorth.dto.otp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;

/**
 * DTO for {@link AppUser}
 */
public record EmailOtpRequest(
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "enter a valid email")
        @NotBlank(message = "Email cannot be empty")
        String email) {

}