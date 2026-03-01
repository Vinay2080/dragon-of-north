package org.miniProjectTwo.DragonOfNorth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.dto.OAuth.OAuthLoginRequest;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.OAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/v1/auth/oauth")
@RequiredArgsConstructor
@Tag(name = "OAuth Authentication", description = "Google OAuth authentication endpoints")
public class OAuthController {

    private final OAuthService oAuthService;

    @PostMapping("/google")
    @Operation(summary = "Authenticate with Google OAuth",
            description = "Authenticates user using Google ID token and sets auth cookies")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<?>> authenticateWithGoogle(
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
        logOAuthRequestDiagnostics("/google", request);
        oAuthService.authenticatedWithGoogle(request.idToken(), request.deviceId(), request.expectedIdentifier(), httpRequest, httpResponse);
        return ResponseEntity.ok(org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse.successMessage("OAuth authentication successful"));
    }

    private void logOAuthRequestDiagnostics(String endpoint, OAuthLoginRequest request) {
        String token = request.idToken();
        int tokenLength = token == null ? 0 : token.length();
        String tokenPrefix = token == null ? "null" : token.substring(0, Math.min(12, tokenLength));

        log.info("OAuth request diagnostics: endpoint={}, idTokenPresent={}, idTokenLength={}, idTokenPrefix={}, deviceIdPresent={}, expectedIdentifierPresent={}",
                endpoint,
                token != null,
                tokenLength,
                tokenPrefix,
                request.deviceId() != null && !request.deviceId().isBlank(),
                request.expectedIdentifier() != null && !request.expectedIdentifier().isBlank());
    }

    @PostMapping("/google/signup")
    @Operation(summary = "Sign up with Google OAuth",
            description = "Creates a new account from Google ID token and sets auth cookies")
    @ApiResponse(responseCode = "200", description = "Signup successful")
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<?>> signupWithGoogle(
            @RequestBody
            @Valid
            OAuthLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        logOAuthRequestDiagnostics("/google/signup", request);
        oAuthService.signupWithGoogle(request.idToken(), request.deviceId(), request.expectedIdentifier(), httpRequest, httpResponse);
        return ResponseEntity.ok(org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse.successMessage("OAuth signup successful"));
    }
}
