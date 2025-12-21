package org.miniProjectTwo.DragonOfNorth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceResolver resolver;

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
            AppUserSignUpRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.signUpUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

}
