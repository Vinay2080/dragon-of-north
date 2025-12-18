package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for {@link org.miniProjectTwo.DragonOfNorth.model.AppUser}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdentifierEmail(
        @Pattern(message = "Email is invalid", regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
        @NotBlank(message = "Email cannot be blank.")
        String email,

        @Size(message = "password should be in between 8 to 50 characters.", min = 8, max = 50)
        @Pattern(message = "password should contain at least one small letter, " +
                "one capital letter, one number and a special character",
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")
        @NotBlank(message = "password cannot be empty.")
        String password) {
}