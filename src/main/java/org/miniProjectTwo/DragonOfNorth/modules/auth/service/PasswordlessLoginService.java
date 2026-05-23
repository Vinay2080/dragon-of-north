package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;

public interface PasswordlessLoginService {

    void requestPasswordlessLogin(String email);

    MfaOrchestrationResult verifyPasswordlessLogin(String token, AuthRequestContext context, HttpServletResponse response);
}
