package org.miniProjectTwo.DragonOfNorth.modules.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;

/**
 * Passwordless login lifecycle contract.
 * <p>
 * Request step issues one-time signed links; verify step validates token freshness, resolves user,
 * and enters the common post-auth issuance pipeline (session cookies or MFA challenge).
 */
public interface PasswordlessLoginService {

    void requestPasswordlessLogin(String email);

    MfaOrchestrationResult verifyPasswordlessLogin(String token, AuthRequestContext context, HttpServletResponse response);
}
