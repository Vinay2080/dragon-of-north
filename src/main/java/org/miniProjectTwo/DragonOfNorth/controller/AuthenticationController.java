package org.miniProjectTwo.DragonOfNorth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserLoginRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpCompleteRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceResolver resolver;
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
            AppUserSignUpRequest request
    ) {

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
    public ResponseEntity<ApiResponse<?>> loginUser(
            @RequestBody
            @Valid
            AppUserLoginRequest request,
            HttpServletResponse httpServletResponse
    ) {
        authCommonServices.login(request.identifier(), request.password(), httpServletResponse);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.successMessage("log in successful"));
    }

    @PostMapping("/jwt/refresh")
    public ResponseEntity<ApiResponse<?>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authCommonServices.refreshToken(request, response);
        return ResponseEntity.ok(ApiResponse.successMessage("refresh token sent"));
    }
}
