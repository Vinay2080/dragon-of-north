package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

public record PasswordResetRequestOtpRequest(
        @JsonAlias({"email", "phone"})
        @NotBlank String identifier,
        @JsonAlias("identifier_type")
        @NotNull IdentifierType identifierType
) {
}
