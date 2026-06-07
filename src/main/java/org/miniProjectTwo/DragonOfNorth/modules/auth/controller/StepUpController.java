package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.MfaVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaChallengeResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.security.web.RequireRecentMfa;
import org.miniProjectTwo.DragonOfNorth.security.web.SensitiveAccountOperation;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.success;
import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.successMessage;

/**
 * Handles the step-up MFA flow that elevates an authenticated session to "recently MFA-verified"
 * status before sensitive operations are allowed to proceed.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Sensitive endpoint is annotated with {@link RequireRecentMfa} and returns 403 when the
 *       session's {@code mfa_verified_at} is stale or absent.</li>
 *   <li>Client calls {@code POST /api/v1/auth/step-up/mfa/request} to get a challenge token.</li>
 *   <li>Client presents the TOTP / recovery-code at {@code POST /api/v1/auth/step-up/mfa/verify}.</li>
 *   <li>On success, the session's {@code mfa_verified_at} is updated and a refreshed access-token
 *       cookie is written — no new session is created, no temporary JWT is issued.</li>
 * </ol>
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li>Reuses the existing {@link org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.service.MfaChallengeService}
 *       so there is one challenge lifecycle, not two.</li>
 *   <li>Session truth is updated in one place ({@link org.miniProjectTwo.DragonOfNorth.modules.session.service.SessionService#refreshMfaVerifiedAt})
 *       and the JWT is re-minted from the authoritative row.</li>
 *   <li>The session itself is not replaced, so existing refresh-token rotation and device binding
 *       continue to function unchanged.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth/step-up")
@RequiredArgsConstructor
public class StepUpController {

    private final AuthCommonServices authCommonServices;

    /**
     * Issues a step-up MFA challenge for the currently authenticated user.
     *
     * <p>Requires a valid access-token (the user must be authenticated).  Returns the same
     * {@link MfaChallengeResponse} shape used by login-time MFA so clients need only one
     * challenge-handling path.</p>
     */
    @PostMapping("/mfa/request")
    public ResponseEntity<ApiResponse<MfaChallengeResponse>> requestStepUp(
            @RequestBody @Valid DeviceIdRequest deviceIdRequest,
            HttpServletRequest httpServletRequest) {

        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpServletRequest, deviceIdRequest.deviceId());
        AppUser user = authCommonServices.findAuthenticatedUser();
        UUID sessionId = resolveSessionId();
        MfaChallenge challenge = authCommonServices.issueStepUpChallenge(user, sessionId, context);
        return ResponseEntity.ok(success(MfaChallengeResponse.from(challenge)));
    }

    /**
     * Verifies a step-up MFA challenge and refreshes the session's {@code mfa_verified_at}.
     *
     * <p>On success:
     * <ul>
     *   <li>The session row's {@code mfa_verified_at} is set to the current time.</li>
     *   <li>A new access-token cookie is written that reflects the refreshed MFA timestamp.</li>
     * </ul>
     * After this call, the client may retry the originally blocked sensitive operation.</p>
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<?>> verifyStepUp(
            @RequestBody @Valid MfaVerifyRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpServletRequest, request.deviceId());
        UUID sessionId = resolveSessionId();
        authCommonServices.completeStepUpMfaChallenge(
                request.challengeId(),
                request.providerType(),
                request.code(),
                sessionId,
                httpServletResponse,
                context
        );
        return ResponseEntity.ok(successMessage("step-up mfa verification successful"));
    }

    /**
     * Demonstrates a sensitive endpoint that requires recent MFA before proceeding.
     *
     * <p>This endpoint illustrates how {@link RequireRecentMfa} is used as a centralized guard
     * at any sensitive operation — the same pattern can be applied to disable-MFA,
     * password-change, account-deletion, and other security-critical actions.</p>
     */
    @PostMapping("/protected-action")
    @SensitiveAccountOperation
    public ResponseEntity<ApiResponse<?>> sensitiveAction(
            @RequestBody @Valid DeviceIdRequest deviceIdRequest,
            HttpServletRequest httpServletRequest) {

        return ResponseEntity.ok(successMessage("sensitive action completed"));
    }

    // ------------------------------------------------------------------ helpers

    private UUID resolveSessionId() {
        SecurityPrincipal principal = resolveSecurityPrincipal();
        UUID sessionId = principal.sessionId();
        if (sessionId == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "Session ID missing from token claims");
        }
        return sessionId;
    }

    private SecurityPrincipal resolveSecurityPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityPrincipal principal)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "User not authenticated");
        }
        return principal;
    }
}
//TODO REMOVE THE TRIAL ENDPOINT...