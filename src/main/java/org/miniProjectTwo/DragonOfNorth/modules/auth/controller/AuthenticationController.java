package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.*;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.success;
import static org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.successMessage;
import static org.springframework.http.HttpStatus.CREATED;

/**
 * REST controller for authentication operations.
 *
 * <h2>API Index</h2>
 * <p>Quick navigation to the endpoints implemented by this controller. Click the method
 * names to jump to the implementation below:</p>
 * <ul>
 *   <li>{@link #findUserStatus(org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserStatusFinderRequest)}
 *       - POST <code>/api/v1/auth/identifier/status</code></li>
 *   <li>{@link #signupUser(org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserSignUpRequest)}
 *       - POST <code>/api/v1/auth/identifier/sign-up</code></li>
 *   <li>{@link #completeUserSignup(org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserSignUpCompleteRequest)}
 *       - POST <code>/api/v1/auth/identifier/sign-up/complete</code></li>
 *   <li>{@link #loginUser(org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.AppUserLoginRequest, jakarta.servlet.http.HttpServletResponse, jakarta.servlet.http.HttpServletRequest)}
 *       - POST <code>/api/v1/auth/identifier/login</code></li>
 *   <li>{@link #refreshToken(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.DeviceIdRequest)}
 *       - POST <code>/api/v1/auth/jwt/refresh</code></li>
 *   <li>{@link #logoutUser(jakarta.servlet.http.HttpServletResponse, jakarta.servlet.http.HttpServletRequest, org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.DeviceIdRequest)}
 *       - POST <code>/api/v1/auth/identifier/logout</code></li>
 *   <li>
 *       - POST <code>/api/v1/auth/password/forgot/request</code></li>
 *   <li>{@link #resetPassword(org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.PasswordResetConfirmRequest)}
 *       - POST <code>/api/v1/auth/password/forgot/reset</code></li>
 * </ul>
 *
 * <p>The list above provides a quick index for new maintainers; each {@code @link} points
 * to the corresponding handler method in this class.</p>
 **/
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Identifier based sign-up, login, logout, and JWT refresh endpoints")
public class AuthenticationController {

    private final AuthenticationServiceResolver resolver;
    private final AuthCommonServices authCommonServices;

    @PostMapping("/identifier/status")
    @Operation(summary = "Check user status by identifier",
            description = "Returns current account lifecycle state for a given identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<AppUserStatusFinderResponse>> findUserStatus(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identifier and type to inspect",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "identifier": "shaking.121@gmail.com",
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
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<AppUserStatusFinderResponse>> signupUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Registration payload",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "identifier": "shaking.121@gmail.com",
                              "identifier_type": "EMAIL",
                              "password": "Example@123"
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
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<AppUserStatusFinderResponse>> completeUserSignup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identifier to finalize",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "identifier": "shaking.121@gmail.com",
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
    @Operation(summary = "Login and issue auth cookies",
            description = "Authenticates credentials and sets access_token + refresh_token HTTP-only cookies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "423", description = "User blocked")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> loginUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Login credentials",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "identifier": "shaking.121@gmail.com",
                              "password": "Example@123",
                              "device_id": "web-chrome-macos"
                            }
                            """)))
            @RequestBody
            @Valid
            AppUserLoginRequest request,
            @Parameter(hidden = true) HttpServletResponse httpServletResponse,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    ) {
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(httpServletRequest, request.deviceId());
        authCommonServices.login(request.identifier(), request.password(), httpServletResponse, context);
        return ResponseEntity.status(HttpStatus.OK).body(successMessage("log in successful"));
    }

    @PostMapping("/jwt/refresh")
    @Operation(summary = "Refresh access token",
            description = "Uses refresh_token cookie + device id to rotate tokens and return new auth cookies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "401", description = "Refresh token missing/invalid/expired")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> refreshToken(
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
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceIdRequest.deviceId());
        authCommonServices.refreshToken(extractRefreshToken(request), response, context);
        return ResponseEntity.ok(successMessage("refresh token sent"));
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("refresh_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @PostMapping("/password/forgot/request")
    @Operation(
            summary = "Request password reset OTP",
            description = "Generates and sends OTP for PASSWORD_RESET purpose to email or phone."
    )
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> requestPasswordResetOtp(
            @Valid
            @RequestBody
            PasswordResetRequestOtpRequest request
    ) {
        authCommonServices.requestPasswordResetOtp(request.identifier(), request.identifierType());
        return ResponseEntity.ok(successMessage("If an account exists, you’ll receive reset instructions."));
    }

    @PostMapping("/password/forgot/reset")
    @Operation(
            summary = "Reset password using OTP",
            description = "Validates PASSWORD_RESET OTP and updates password. Revokes all active sessions."
    )
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> resetPassword(
            @Valid @org.springframework.web.bind.annotation.RequestBody PasswordResetConfirmRequest request
    ) {
        authCommonServices.resetPassword(request);
        return ResponseEntity.ok(successMessage("password reset successful"));
    }

    @PostMapping("/identifier/logout")
    @Operation(summary = "Logout current device",
            description = "Revokes current device session and clears authentication cookies.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> logoutUser(
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
        AuthRequestContext context = AuthRequestContext.fromHttpRequest(request, deviceIdRequest.deviceId());
        authCommonServices.logoutUser(extractRefreshToken(request), response, context);
        return ResponseEntity.ok(successMessage("user logged out successfully"));
    }

    @PostMapping("/password/change")
    @Operation(
            summary = "Change password",
            description = "Validates old password and updates it for the authenticated user. Revokes all active sessions."
    )
    public ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> changePassword(
            @Valid @org.springframework.web.bind.annotation.RequestBody PasswordChangeRequest request
    ) {
        authCommonServices.changePassword(request);
        return ResponseEntity.ok(successMessage("password change successful"));
    }
}
