package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupConfirmResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupResponse;

public interface MfaService {

    MfaSetupResponse requestMfaSetup(AuthRequestContext context);

    MfaSetupConfirmResponse confirmMfaSetup(AuthRequestContext context, @NotNull @Length String code);
}
