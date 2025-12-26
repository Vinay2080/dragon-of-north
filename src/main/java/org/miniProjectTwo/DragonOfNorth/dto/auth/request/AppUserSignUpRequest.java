package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

/**
 * DTO for {@link org.miniProjectTwo.DragonOfNorth.model.AppUser}
 */
public record AppUserSignUpRequest(

        @NotBlank
        String identifier,

        @NotNull(message = "password cannot be null")
        IdentifierType identifierType,

        @Size(message = "password length must be between 8 and 50", min = 8, max = 50)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", message = """
                At least one lowercase letter
                At least one uppercase letter
                At least one number
                At least one special character""")
        @NotBlank(message = "password cannot be blank")
        String password) {
}