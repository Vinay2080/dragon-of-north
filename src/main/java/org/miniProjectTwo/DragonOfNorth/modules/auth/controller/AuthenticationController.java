package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.api.AuthenticationApi;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.*;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaChallengeResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupConfirmResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.*;
import org.miniProjectTwo.DragonOfNorth.security.web.RequireRecentMfa;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.success;
import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.successMessage;
import static org.springframework.http.HttpStatus.CREATED;

/**
 * Authentication HTTP controller that coordinates identifier-based authentication flows.
 * <p>
 * This controller intentionally remains thin and delegates business logic to domain services.
 * Several endpoints orchestrate multistep flows (for example, login + MFA challenge and
 * sign-up + OTP verification + completion), so the method contracts are kept stable to
 * make future refactoring safer without breaking API behavior.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController implements AuthenticationApi {

    private final AuthenticationServiceResolver resolver;
    private final AuthCommonServices authCommonServices;
    private final PasswordService passwordService;
    private final MfaService mfaService;
    private final PasswordlessLoginService passwordlessLoginService;

    @Override
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<?>> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.ok(successMessage("csrf token ready"));
    }

    @Override
    @PostMapping("/identifier/status")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<AppUserStatusFinderResponse>> findUserStatus(
            @RequestBody @Valid AppUserStatusFinderRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.getUserStatus(request.identifier());
        return ResponseEntity.ok(success(response));
    }

    /**
     * Starts sign-up for an identifier.
     * <p>
     * In production this is typically followed by OTP verification through OTP endpoints
     * and then {@code /identifier/sign-up/complete} to activate the account lifecycle state.
     */
    @Override
    @PostMapping("/identifier/sign-up")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<AppUserStatusFinderResponse>> signupUser(
            @RequestBody @Valid AppUserSignUpRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.signUpUser(request);
        return ResponseEntity.status(CREATED).body(success(response));
    }

    @Override
    @PostMapping("/identifier/sign-up/complete")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<AppUserStatusFinderResponse>> completeUserSignup(
            @RequestBody @Valid AppUserSignUpCompleteRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.completeSignUp(request.identifier());
        return ResponseEntity.status(CREATED).body(success(response));
    }

    /**
     * Performs primary-factor login and conditionally branches into MFA challenge orchestration.
     * <p>
     * When MFA is required, no success message is returned; instead, a challenge payload is
     * returned so clients can continue with the MFA verification flow.
     */
    @Override
    @PostMapping("/identifier/login")
    public ResponseEntity<ApiResponse<?>> loginUser(
            @RequestBody @Valid AppUserLoginRequest request,
            HttpServletResponse httpServletResponse,
            HttpServletRequest httpServletRequest
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpServletRequest, request.deviceId());
        MfaOrchestrationResult result = authCommonServices.login(request.identifier(), request.password(), httpServletResponse, context);
        if (result.challengeRequired()) {
            return ResponseEntity.ok(success(MfaChallengeResponse.from(result.challenge())));
        }
        return ResponseEntity.ok(successMessage("log in successful"));
    }

    @Override
    @PostMapping("/jwt/refresh")
    public ResponseEntity<ApiResponse<?>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody @Valid DeviceIdRequest deviceIdRequest
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceIdRequest.deviceId());
        authCommonServices.refreshToken(extractRefreshToken(request), response, context);
        return ResponseEntity.ok(successMessage("refresh token sent"));
    }

    @Override
    @PostMapping("/password/forgot/request")
    public ResponseEntity<ApiResponse<?>> requestPasswordResetOtp(
            @RequestBody @Valid PasswordResetRequestOtpRequest request
    ) {
        passwordService.requestPasswordResetOtp(request.identifier(), request.identifierType());
        return ResponseEntity.ok(successMessage("If an account exists, you’ll receive reset instructions."));
    }

    @Override
    @PostMapping("/password/forgot/reset")
    public ResponseEntity<ApiResponse<?>> resetPassword(
            @RequestBody @Valid PasswordResetConfirmRequest request
    ) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok(successMessage("password reset successful"));
    }

    @Override
    @PostMapping("/identifier/logout")
    public ResponseEntity<ApiResponse<?>> logoutUser(
            HttpServletResponse response,
            HttpServletRequest request,
            @RequestBody @Valid DeviceIdRequest deviceIdRequest
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceIdRequest.deviceId());
        authCommonServices.logoutUser(extractRefreshToken(request), response, context);
        return ResponseEntity.ok(successMessage("user logged out successfully"));
    }

    @Override
    @PostMapping("/password/change")
    @RequireRecentMfa
    public ResponseEntity<ApiResponse<?>> changePassword(
            @RequestBody @Valid PasswordChangeRequest request
    ) {
        passwordService.changePassword(request);
        return ResponseEntity.ok(successMessage("password change successful"));
    }

    @Override
    @PostMapping("/account/delete")
    @RequireRecentMfa
    public ResponseEntity<ApiResponse<?>> deleteAccount(
            @RequestBody @Valid DeviceIdRequest deviceIdRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceIdRequest.deviceId());
        authCommonServices.deleteAccount(response, context);
        return ResponseEntity.ok(successMessage("account deleted successfully"));
    }

    @Override
    @PostMapping({"/passwordless/request", "/login/passwordless/request"})
    public ResponseEntity<ApiResponse<?>> requestPasswordlessLogin(
            @RequestBody @Valid RequestPasswordlessLoginDto passwordlessLoginDto) {
        passwordlessLoginService.requestPasswordlessLogin(passwordlessLoginDto.email());
        return ResponseEntity.ok(successMessage("Passwordless login link sent if the email is registered"));
    }

    /**
     * Verifies passwordless token and applies the same post-auth orchestration as password login.
     * <p>
     * This endpoint may also return an MFA challenge, so callers must handle both terminal
     * login success and challenge-required responses.
     */
    @Override
    @PostMapping({"/passwordless/verify", "/login/passwordless/verify"})
    public ResponseEntity<ApiResponse<?>> verifyPasswordlessLogin(
            @RequestBody @Valid VerifyPasswordlessLoginDto verifyPasswordlessLoginDto,
            HttpServletRequest request,
            HttpServletResponse response) {

        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, verifyPasswordlessLoginDto.deviceId());
        MfaOrchestrationResult result = passwordlessLoginService.verifyPasswordlessLogin(verifyPasswordlessLoginDto.token(), context, response);
        if (result.challengeRequired()) {
            return ResponseEntity.ok(success(MfaChallengeResponse.from(result.challenge())));
        }
        return ResponseEntity.ok(successMessage("Passwordless login successful"));
    }

    @Override
    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<?>> verifyMfaChallenge(
            @RequestBody @Valid MfaVerifyRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpServletRequest, request.deviceId());
        authCommonServices.completeMfaChallengeLogin(
                request.challengeId(),
                request.code(),
                request.providerType(),
                httpServletResponse,
                context
        );
        return ResponseEntity.ok(successMessage("mfa verification successful"));
    }

    /**
     * Starts TOTP setup for an authenticated account and returns bootstrap material.
     * <p>
     * The setup is not persisted as enabled until {@link #confirmMfaSetup(HttpServletRequest, MfaSetupConfirmRequest)}
     * succeeds with a valid authenticator code.
     */
    @PostMapping("/enable/mfa/request")
    @Override
    public ResponseEntity<ApiResponse<MfaSetupResponse>> requestMfaSetup(
            HttpServletRequest request,
            @RequestBody @Valid DeviceIdRequest deviceIdRequest) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceIdRequest.deviceId());
        MfaSetupResponse mfaSetupResponse = mfaService.requestMfaSetup(context);
        return ResponseEntity.ok(success(mfaSetupResponse));
    }

    @Override
    @PostMapping("/enable/mfa/confirm")
    public ResponseEntity<ApiResponse<MfaSetupConfirmResponse>> confirmMfaSetup(
            HttpServletRequest request,
            @RequestBody @Valid MfaSetupConfirmRequest mfaSetupConfirmRequest) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, mfaSetupConfirmRequest.deviceId());
        MfaSetupConfirmResponse codes = mfaService.confirmMfaSetup(context, mfaSetupConfirmRequest.code());
        return ResponseEntity.ok(success(codes));
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
