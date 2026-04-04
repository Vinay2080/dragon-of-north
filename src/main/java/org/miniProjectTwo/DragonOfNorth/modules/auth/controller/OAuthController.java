package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.api.OAuthApi;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.OAuthService;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthLoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.successMessage;

@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class OAuthController implements OAuthApi {

    private final OAuthService oAuthService;

    @Override
    @PostMapping("/google")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> authenticateWithGoogle(
            @RequestBody @Valid OAuthLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        oAuthService.authenticatedWithGoogle(
                request.idToken(),
                request.deviceId(),
                request.expectedIdentifier(),
                httpRequest,
                httpResponse
        );
        return ResponseEntity.ok(successMessage("OAuth authentication successful"));
    }

    @Override
    @PostMapping("/google/signup")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> signupWithGoogle(
            @RequestBody @Valid OAuthLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        oAuthService.signupWithGoogle(
                request.idToken(),
                request.deviceId(),
                request.expectedIdentifier(),
                httpRequest,
                httpResponse
        );
        return ResponseEntity.ok(successMessage("OAuth signup successful"));
    }
}
