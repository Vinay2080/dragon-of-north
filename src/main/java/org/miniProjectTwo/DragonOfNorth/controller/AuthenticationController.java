package org.miniProjectTwo.DragonOfNorth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.components.SignupRateLimiter;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpCompleteRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
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

    @GetMapping("/identifier/status")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> findUserStatus(
            @RequestBody
            @Valid
            AppUserStatusFinderRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.getUserStatus(request.identifier());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/identier/sign-up")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> signupUser(
            @RequestBody
            @Valid
            AppUserSignUpRequest request,
            HttpServletRequest httpServletRequest) {
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
}
