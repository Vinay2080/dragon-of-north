package org.miniProjectTwo.DragonOfNorth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.*;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse.success;
import static org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse.successMessage;
import static org.springframework.http.HttpStatus.CREATED;

/**
 * REST controller for authentication operations.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Identifier based sign-up, login, logout, and JWT refresh endpoints")
public class AuthenticationController {

    private final AuthenticationServiceResolver resolver;
    private final AuthCommonServices authCommonServices;

    @PostMapping("/identifier/status")
    @Operation(summary = "Check user status by identifier", description = "Returns current account lifecycle state for a given identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<AppUserStatusFinderResponse>> findUserStatus(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identifier and type to inspect",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "identifier": "intern.candidate@example.com",
                              "identifier_type": "EMAIL"
                            }
                            """)))
            @RequestBody
            @Valid
            AppUserStatusFinderRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.getUserStatus(request.identifier());
        return ResponseEntity.ok(success(response));
    }

    @PostMapping("/identifier/sign-up")
    @Operation(summary = "Start sign-up", description = "Creates user in CREATED status and starts verification flow.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sign-up started"),
            @ApiResponse(responseCode = "409", description = "Identifier already exists"),
            @ApiResponse(responseCode = "400", description = "Validation failure")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<AppUserStatusFinderResponse>> signupUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Registration payload",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "identifier": "intern.candidate@example.com",
                              "identifier_type": "EMAIL",
                              "password": "Intern@123"
                            }
                            """)))
            @RequestBody
            @Valid
            AppUserSignUpRequest request
    ) {

        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.signUpUser(request);
        return ResponseEntity.status(CREATED).body(success(response));
    }

    @PostMapping("/identifier/sign-up/complete")
    @Operation(summary = "Complete sign-up", description = "Marks user as VERIFIED after OTP validation completes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sign-up completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request or not yet verified")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<AppUserStatusFinderResponse>> completeUserSignup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identifier to finalize",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "identifier": "intern.candidate@example.com",
                              "identifier_type": "EMAIL"
                            }
                            """)))
            @RequestBody
            @Valid
            AppUserSignUpCompleteRequest request
    ) {
        AuthenticationService service = resolver.resolve(request.identifier(), request.identifierType());
        AppUserStatusFinderResponse response = service.completeSignUp(request.identifier());
        return ResponseEntity.status(CREATED).body(success(response));
    }

    @PostMapping("/identifier/login")
    @Operation(summary = "Login and issue auth cookies", description = "Authenticates credentials and sets access_token + refresh_token HTTP-only cookies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "423", description = "User blocked")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<?>> loginUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Login credentials",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "identifier": "intern.candidate@example.com",
                              "password": "Intern@123",
                              "device_id": "web-chrome-macos"
                            }
                            """)))
            @RequestBody
            @Valid
            AppUserLoginRequest request,
            @Parameter(hidden = true) HttpServletResponse httpServletResponse,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        authCommonServices.login(request.identifier(), request.password(), httpServletResponse, httpServletRequest, request.deviceId());
        return ResponseEntity.status(HttpStatus.OK).body(successMessage("log in successful"));
    }

    @PostMapping("/jwt/refresh")
    @Operation(summary = "Refresh access token", description = "Uses refresh_token cookie + device id to rotate tokens and return new auth cookies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "401", description = "Refresh token missing/invalid/expired")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<?>> refreshToken(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Device id associated with the session",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "device_id": "web-chrome-macos"
                            }
                            """)))
            @RequestBody
            @Valid
            DeviceIdRequest deviceIdRequest
    ) {
        authCommonServices.refreshToken(request, response, deviceIdRequest.deviceId());
        return ResponseEntity.ok(successMessage("refresh token sent"));
    }

    @PostMapping("/identifier/logout")
    @Operation(summary = "Logout current device", description = "Revokes current device session and clears authentication cookies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse<?>> logoutUser(
            @Parameter(hidden = true) HttpServletResponse response,
            @Parameter(hidden = true) HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Device id to revoke",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "device_id": "web-chrome-macos"
                            }
                            """)))
            @RequestBody
            @Valid
            DeviceIdRequest deviceIdRequest
    ) {
        authCommonServices.logoutUser(request, response, deviceIdRequest.deviceId());
        return ResponseEntity.ok(successMessage("user logged out successfully"));
    }
}
