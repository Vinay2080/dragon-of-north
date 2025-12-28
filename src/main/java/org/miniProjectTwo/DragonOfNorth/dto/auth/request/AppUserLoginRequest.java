package org.miniProjectTwo.DragonOfNorth.dto.auth.request;

import jakarta.validation.constraints.NotBlank;

public record AppUserLoginRequest(
        @NotBlank
        String identifier,

        @NotBlank(message = "password cannot be blank")
        String password) {

}