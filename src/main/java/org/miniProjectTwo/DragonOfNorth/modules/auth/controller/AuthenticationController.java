package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.api.AuthenticationApi;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.*;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthenticationService;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.success;
import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.successMessage;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController implements AuthenticationApi {

    private final AuthenticationServiceResolver resolver;
    private final AuthCommonServices authCommonServices;

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

    @Override
    @PostMapping("/identifier/login")
    public ResponseEntity<ApiResponse<?>> loginUser(
            @RequestBody @Valid AppUserLoginRequest request,
            HttpServletResponse httpServletResponse,
            HttpServletRequest httpServletRequest
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpServletRequest, request.deviceId());
        authCommonServices.login(request.identifier(), request.password(), httpServletResponse, context);
        return ResponseEntity.status(HttpStatus.OK).body(successMessage("log in successful"));
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
        authCommonServices.requestPasswordResetOtp(request.identifier(), request.identifierType());
        return ResponseEntity.ok(successMessage("If an account exists, you’ll receive reset instructions."));
    }

    @Override
    @PostMapping("/password/forgot/reset")
    public ResponseEntity<ApiResponse<?>> resetPassword(
            @RequestBody @Valid PasswordResetConfirmRequest request
    ) {
        authCommonServices.resetPassword(request);
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
    public ResponseEntity<ApiResponse<?>> changePassword(
            @RequestBody @Valid PasswordChangeRequest request
    ) {
        authCommonServices.changePassword(request);
        return ResponseEntity.ok(successMessage("password change successful"));
    }

    @Override
    @PostMapping("/account/delete")
    public ResponseEntity<ApiResponse<?>> deleteAccount(
            @RequestBody @Valid DeviceIdRequest deviceIdRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceIdRequest.deviceId());
        authCommonServices.deleteAccount(response, context);
        return ResponseEntity.ok(successMessage("account deleted successfully"));
    }

    // http request is not currently being used.
    @Override
    @PostMapping("/passwordless/request")
    public ResponseEntity<ApiResponse<?>> requestPasswordlessLogin(
            @RequestBody @Valid RequestPasswordlessLoginDto passwordlessLoginDto) {
        authCommonServices.requestPasswordlessLogin(passwordlessLoginDto.email());
        return ResponseEntity.ok(successMessage("Passwordless login link sent if the email is registered"));
    }

    @Override
    @PostMapping("/passwordless/verify")
    public ResponseEntity<ApiResponse<?>> VerifyPasswordlessLogin(
            @RequestBody @Valid VerifyPasswordlessLoginDto verifyPasswordlessLoginDto,
            HttpServletRequest request,
            HttpServletResponse response) {

        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, verifyPasswordlessLoginDto.deviceId());
        authCommonServices.verifyPasswordlessLogin(verifyPasswordlessLoginDto.token(), context, response);
        return ResponseEntity.ok(successMessage("Passwordless login successful"));
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
