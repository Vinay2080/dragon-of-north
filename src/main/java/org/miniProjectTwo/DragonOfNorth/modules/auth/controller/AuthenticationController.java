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
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.VerificationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaPolicy;
import org.miniProjectTwo.DragonOfNorth.modules.auth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.*;
import org.miniProjectTwo.DragonOfNorth.security.web.RequireRecentMfa;
import org.miniProjectTwo.DragonOfNorth.security.web.SensitiveAccountOperation;
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

    /**
     * Retrieves CSRF token for the current session.
     *
     * @param csrfToken CSRF token from the request
     * @return ResponseEntity with success message and CSRF token
     */
    @Override
    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<?>> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.ok(successMessage("csrf token ready"));
    }

    /**
     * Retrieves user status for the provided identifier.
     * Called when user status needs to be checked.
     * To determine if a user exists, is verified, and other status details.
     * Allowing user to redirect to log in or sign-up based on status.
     *
     * @param request Identifier and type for user status lookup
     * @return ResponseEntity with user status information
     * @throws RuntimeException if a user status lookup fails
     */
    @Override
    @PostMapping("/identifier/status")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> findUserStatus(
            @RequestBody @Valid AppUserStatusFinderRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.getUserStatus(request.identifier());
        return ResponseEntity.ok(success(response));
    }

    /**
     * Starts sign-up for an identifier.
     * Called if the user does not exist or is not verified.
     * <p>
     * In production this is typically followed by OTP verification through OTP endpoints
     * and then {@code /identifier/sign-up/complete} to activate the account lifecycle state.
     */
    @Override
    @PostMapping("/identifier/sign-up")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> signupUser(
            @RequestBody @Valid AppUserSignUpRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.signUpUser(request);
        return ResponseEntity.status(CREATED).body(success(response));
    }

    /**
     * Completes a user sign-up process.
     * Called after email verification and OTP verification.
     *
     * @param request Identifier and type for user status lookup
     * @return ResponseEntity with user status information
     * @throws RuntimeException if a user signup completion fails
     */
    @Override
    @PostMapping("/identifier/sign-up/complete")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> completeUserSignup(
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

    /**
     * Refreshes the JWT token for the authenticated user.
     * Called when a 403 error occurs due to an expired token.
     *
     * @param httpServletRequest  HTTP servlet request
     * @param httpServletResponse HTTP servlet response
     * @param deviceIdRequest     Device ID request for authentication context
     * @return ResponseEntity with a success message or challenge response
     */
    @Override
    @PostMapping("/jwt/refresh")
    public ResponseEntity<ApiResponse<?>> refreshToken(
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            @RequestBody @Valid DeviceIdRequest deviceIdRequest
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpServletRequest, deviceIdRequest.deviceId());
        authCommonServices.refreshToken(extractRefreshToken(httpServletRequest), httpServletResponse, context);
        return ResponseEntity.ok(successMessage("refresh token sent"));
    }

    /**
     * Handles password reset request by sending OTP to the user's identifier.
     * Generates OTP and sends it to the user's identifier.
     *
     * @param request Password reset request with identifier and type
     * @return ResponseEntity with a success message
     */
    @Override
    @PostMapping("/password/forgot/request")
    public ResponseEntity<ApiResponse<?>> requestPasswordResetOtp(
            @RequestBody @Valid PasswordResetRequestOtpRequest request
    ) {
        passwordService.requestPasswordResetOtp(request.identifier(), request.identifierType());
        return ResponseEntity.ok(successMessage("If an account exists, you’ll receive reset instructions."));
    }

    /**
     * Handles password reset confirmation by validating OTP and updating password.
     * @param request Password reset confirmation request with identifier, type, and new password
     * @return ResponseEntity with a success message
     */
    @Override
    @PostMapping("/password/forgot/reset")
    public ResponseEntity<ApiResponse<?>> resetPassword(
            @RequestBody @Valid PasswordResetConfirmRequest request
    ) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok(successMessage("password reset successful"));
    }

    /**
     * Handles user logout by invalidating the refresh token and clearing the session.
     * @param response HttpServletResponse for setting cookies
     * @param request HttpServletRequest for extracting device ID
     * @param deviceIdRequest Device ID request containing device ID
     * @return ResponseEntity with a success message
     */
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

    /**
     * Handles password change request by validating the current password and updating the new password.
     * @param request Password change request with current password and new password
     * @return ResponseEntity with a success message
     */
    @Override
    @PostMapping("/password/change")
    @SensitiveAccountOperation(policy = RecentMfaPolicy.PASSWORD_CHANGE)
    public ResponseEntity<ApiResponse<?>> changePassword(
            @RequestBody @Valid PasswordChangeRequest request
    ) {
        passwordService.changePassword(request);
        return ResponseEntity.ok(successMessage("password change successful"));
    }

    /**
     * Handles user account deletion and invalidates refresh token and clears session.
     *
     * @param deviceIdRequest Device ID request containing device ID
     * @param request HttpServletRequest for extracting device ID
     * @param response HttpServletResponse for setting cookies
     * @return ResponseEntity with a success message
     */
    @Override
    @PostMapping("/account/delete")
    @SensitiveAccountOperation(policy = RecentMfaPolicy.ACCOUNT_DELETE)
    public ResponseEntity<ApiResponse<?>> deleteAccount(
            @RequestBody @Valid DeviceIdRequest deviceIdRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceIdRequest.deviceId());
        authCommonServices.deleteAccount(response, context);
        return ResponseEntity.ok(successMessage("account deleted successfully"));
    }

    /**
     *  Allows user to login without a password using a passwordless token.
     *  Link is sent to the user's registered email.
     * @param passwordlessLoginDto Passwordless login DTO containing email
     * @return ResponseEntity with a success message
     */
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

    /**
     *  Handles verification of a Multi-Factor Authentication (MFA) challenge.
     * @param request MFA verification request containing challenge ID and code
     * @param httpServletRequest HttpServletRequest for extracting device ID
     * @param httpServletResponse HttpServletResponse for setting cookies
     * @return ResponseEntity with MFA challenge response or success message
     */
    @Override
    @PostMapping("/mfa/verify")
    public ResponseEntity<ApiResponse<?>> verifyMfaChallenge(
            @RequestBody @Valid MfaVerifyRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpServletRequest, request.deviceId());
        VerificationResult result = authCommonServices.completeMfaChallengeLogin(
                request.challengeId(),
                request.code(),
                request.providerType(),
                httpServletResponse,
                context
        );
        if (result.success()) {
            return ResponseEntity.ok(successMessage("mfa verification successful"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.failed(result.failureReason()));
        }
    }

    /**
     * Starts TOTP setup for an authenticated account and returns bootstrap material.
     * <p>
     * The setup is not persisted as enabled until {@link #confirmMfaSetup(HttpServletRequest, MfaSetupConfirmRequest)}
     * succeeds with a valid authenticator code.
     */
    @PostMapping("/enable/mfa/request")
    @RequireRecentMfa(onlyWhenMfaEnabled = true)
    @Override
    public ResponseEntity<ApiResponse<MfaSetupResponse>> requestMfaSetup(
            HttpServletRequest request,
            @RequestBody @Valid DeviceIdRequest deviceIdRequest) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceIdRequest.deviceId());
        MfaSetupResponse mfaSetupResponse = mfaService.requestMfaSetup(context);
        return ResponseEntity.ok(success(mfaSetupResponse));
    }

    /**
     * Confirms TOTP setup for an authenticated account with a valid authenticator code.
     * <p>
     * The setup is persisted as enabled only if the provided code is valid.
     *
     * @param request HTTP request containing device ID
     * @param mfaSetupConfirmRequest TOTP setup confirmation request
     * @return ResponseEntity with MFA setup confirmation response or failure message
     */
    @Override
    @PostMapping("/enable/mfa/confirm")
    @RequireRecentMfa(onlyWhenMfaEnabled = true)
    public ResponseEntity<ApiResponse<MfaSetupConfirmResponse>> confirmMfaSetup(
            HttpServletRequest request,
            @RequestBody @Valid MfaSetupConfirmRequest mfaSetupConfirmRequest) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, mfaSetupConfirmRequest.deviceId());
        MfaSetupConfirmResponse codes = mfaService.confirmMfaSetup(context, mfaSetupConfirmRequest.code());
        return ResponseEntity.ok(success(codes));
    }

    /**
     * Extracts the refresh token from the HTTP request cookies.
     *
     * @param request HTTP request containing cookies
     * @return Refresh token value or null if not found
     */
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
