package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.OAuthService;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthLoginRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.successMessage;

/**
 * OAuth endpoints for Google login and signup.
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth Authentication", description = "Google OAuth authentication endpoints")
public class OAuthController {

    private final OAuthService oAuthService;

    @PostMapping("/google")
    @Operation(summary = "Authenticate with Google OAuth",
            description = "Authenticates user using Google ID token and sets auth cookies")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> authenticateWithGoogle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Google OAuth login request",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                                "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij..."
                            }
                            """)))
            @RequestBody
            @Valid
            OAuthLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        oAuthService.authenticatedWithGoogle(request.idToken(), request.deviceId(), request.expectedIdentifier(), httpRequest, httpResponse);
        return ResponseEntity.ok(successMessage("OAuth authentication successful"));
    }

    @PostMapping("/google/signup")
    @Operation(summary = "Sign up with Google OAuth",
            description = "Creates a new account from Google ID token and sets auth cookies")
    @ApiResponse(responseCode = "200", description = "Signup successful")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> signupWithGoogle(
            @RequestBody
            @Valid
            OAuthLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        oAuthService.signupWithGoogle(request.idToken(), request.deviceId(), request.expectedIdentifier(), httpRequest, httpResponse);
        return ResponseEntity.ok(successMessage("OAuth signup successful"));
    }
}
