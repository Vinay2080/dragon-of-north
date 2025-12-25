package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

public record AppUserSignUpCompleteRequest(
        @NotNull String identifier,
        @NotBlank IdentifierType identifierType) {
}
