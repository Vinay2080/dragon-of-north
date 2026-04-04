package org.miniProjectTwo.DragonOfNorth.modules.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthLoginRequest;
import org.springframework.http.ResponseEntity;

@Tag(name = "OAuth Authentication", description = "Google OAuth login and sign-up endpoints.")
public interface OAuthApi {

    @Operation(
            summary = "Log in with Google",
            description = "Authenticates an existing user with a Google ID token and sets the application's auth cookies."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OAuth login successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "googleLoginSuccess",
                                    value = """
                                            {
                                              "message": "OAuth authentication successful",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Google token is invalid or expired"),
            @ApiResponse(responseCode = "409", description = "Account linking confirmation is required")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> authenticateWithGoogle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Google ID token payload for an existing account.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "googleLoginRequest",
                                    value = """
                                            {
                                              "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...",
                                              "device_id": "web-chrome-macos",
                                              "expected_identifier": "intern.candidate@example.com"
                                            }
                                            """
                            )
                    )
            )
            OAuthLoginRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest,
            @Parameter(hidden = true) HttpServletResponse httpResponse
    );

    @Operation(
            summary = "Sign up with Google",
            description = "Creates a new account from a Google ID token and immediately establishes an authenticated session."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OAuth sign-up successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "googleSignupSuccess",
                                    value = """
                                            {
                                              "message": "OAuth signup successful",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Google token is invalid or expired"),
            @ApiResponse(responseCode = "409", description = "An account already exists for the Google email")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> signupWithGoogle(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Google ID token payload for a new account.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "googleSignupRequest",
                                    value = """
                                            {
                                              "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6Ij...",
                                              "device_id": "web-chrome-macos",
                                              "expected_identifier": "intern.candidate@example.com"
                                            }
                                            """
                            )
                    )
            )
            OAuthLoginRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest,
            @Parameter(hidden = true) HttpServletResponse httpResponse
    );
}
