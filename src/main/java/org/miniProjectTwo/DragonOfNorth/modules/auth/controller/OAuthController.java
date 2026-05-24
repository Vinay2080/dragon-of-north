package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.api.OAuthApi;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AuthRequestContext;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaChallengeResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.OAuthService;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthLoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.success;
import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.successMessage;

/**
 * OAuth authentication controller for Google-based sign-in and sign-up flows.
 * <p>
 * Both endpoints can finish authentication immediately or branch into MFA challenge
 * orchestration depending on account policy, so clients should treat the response as
 * either terminal success or challenge continuation payload.
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class OAuthController implements OAuthApi {

    private final OAuthService oAuthService;

    /**
     * Authenticates an existing account using a Google ID token and current device context.
     */
    @Override
    @PostMapping("/google")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> authenticateWithGoogle(
            @RequestBody @Valid OAuthLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpRequest, request.deviceId());
        MfaOrchestrationResult result = oAuthService.authenticatedWithGoogle(
                request.idToken(),
                request.expectedIdentifier(),
                context,
                httpResponse
        );
        if (result.challengeRequired()) {
            return ResponseEntity.ok(success(MfaChallengeResponse.from(result.challenge())));
        }
        return ResponseEntity.ok(successMessage("OAuth authentication successful"));
    }

    /**
     * Creates or links an account using Google identity and applies post-auth MFA orchestration.
     */
    @Override
    @PostMapping("/google/signup")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> signupWithGoogle(
            @RequestBody @Valid OAuthLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpRequest, request.deviceId());
        MfaOrchestrationResult result = oAuthService.signupWithGoogle(
                request.idToken(),
                request.expectedIdentifier(),
                context,
                httpResponse
        );
        if (result.challengeRequired()) {
            return ResponseEntity.ok(success(MfaChallengeResponse.from(result.challenge())));
        }
        return ResponseEntity.ok(successMessage("OAuth signup successful"));
    }
}
