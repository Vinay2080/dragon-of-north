package org.miniProjectTwo.DragonOfNorth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.components.SignupRateLimiter;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.*;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AuthenticationResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.RefreshTokenResponse;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceResolver resolver;
    private final SignupRateLimiter signupRateLimiter;
    private final AuthCommonServices authCommonServices;

    @PostMapping("/identifier/status")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> findUserStatus(
            @RequestBody
            @Valid
            AppUserStatusFinderRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.getUserStatus(request.identifier());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/identifier/sign-up")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> signupUser(
            @RequestBody
            @Valid
            AppUserSignUpRequest request,
            HttpServletRequest httpServletRequest) {
        // todo move it somewhere.
        String ip = httpServletRequest.getHeader("X-Forwarded-For");
        if (ip == null) {
            ip = httpServletRequest.getRemoteAddr();
        }
        signupRateLimiter.check(request.identifier(), ip);
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.signUpUser(request);
        return ResponseEntity.status(CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/identifier/sign-up/complete")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> completeUserSignup(
            @RequestBody
            @Valid
            AppUserSignUpCompleteRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.completeSignUp(request.identifier());
        return ResponseEntity.status(CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/identifier/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> loginUser(
            @RequestBody
            @Valid
            AppUserLoginRequest request,
            HttpServletResponse httpServletResponse
    ) {
        AuthenticationResponse response = authCommonServices.login(request.identifier(), request.password(), httpServletResponse);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/jwt/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid
            @RequestBody
            RefreshTokenRequest request
    ) {
        RefreshTokenResponse response = authCommonServices.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
