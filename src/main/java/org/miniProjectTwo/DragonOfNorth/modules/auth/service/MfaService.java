package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupConfirmResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupResponse;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;

public interface MfaService {

    MfaSetupResponse requestMfaSetup(AuthRequestContext context);

    MfaSetupConfirmResponse confirmMfaSetup(AuthRequestContext context, @NotNull @Length String code);

    boolean verifyMfaCode(AppUser appUser, @NotNull @Length String code);

    boolean verifyTotpCode(AppUser appUser, @NotNull @Length String code);

    boolean verifyRecoveryCode(AppUser appUser, @NotNull @Length String code);
}
