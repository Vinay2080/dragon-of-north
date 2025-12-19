package org.miniProjectTwo.DragonOfNorth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private AuthenticationService emailAuthenticationService;

    public AuthenticationController(
            @Qualifier("emailAuthenticationServiceImpl")
            AuthenticationService authenticationService) {
        this.emailAuthenticationService = authenticationService;
    }

    @GetMapping("/identifier/email")
    public ResponseEntity<ApiResponse<AppUserStatusFinderResponse>> findUserStatus(
            @RequestBody
            @Valid
            AppUserStatusFinderRequest request
    ) {
        AppUserStatusFinderResponse response = emailAuthenticationService.statusFinder(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
