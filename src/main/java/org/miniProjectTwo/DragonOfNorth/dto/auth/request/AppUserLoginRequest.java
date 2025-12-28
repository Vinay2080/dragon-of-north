package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

public record AppUserLoginRequest(
        @NotBlank
        String identifier,

        @NotNull(message = "identifier type cannot be null")
        IdentifierType identifierType,

        @NotBlank(message = "password cannot be blank")
        String password) {

}