package org.miniProjectTwo.DragonOfNorth.modules.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.*;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;

@Tag(name = "Authentication", description = "Identifier-based sign-up, login, password, logout, and token refresh endpoints.")
public interface AuthenticationApi {

    @Operation(
            summary = "Prepare CSRF token",
            description = "Initializes the CSRF token flow so browser clients can include the token in later state-changing requests."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "CSRF token flow initialized",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "csrfReady",
                                    value = """
                                            {
                                              "message": "csrf token ready",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> csrf(
            @Parameter(hidden = true) CsrfToken csrfToken
    );

    @Operation(
            summary = "Check identifier status",
            description = "Returns whether an account exists for the identifier and shows its verification state, linked providers, and lifecycle status."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Identifier status returned",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "statusFound",
                                    value = """
                                            {
                                              "api_response_status": "success",
                                              "data": {
                                                "exists": true,
                                                "providers": ["LOCAL"],
                                                "email_verified": false,
                                                "app_user_status": "CREATED"
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "invalidStatusRequest",
                                    value = """
                                            {
                                              "api_response_status": "failed",
                                              "data": {
                                                "code": "VAL_001",
                                                "validation_error_list": [
                                                  {
                                                    "field": "identifier_type",
                                                    "code": "Identifier type is required",
                                                    "message": "Identifier type is required"
                                                  }
                                                ]
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<AppUserStatusFinderResponse>> findUserStatus(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identifier and identifier type to inspect.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "emailStatusRequest",
                                    value = """
                                            {
                                              "identifier": "intern.candidate@example.com",
                                              "identifier_type": "EMAIL"
                                            }
                                            """
                            )
                    )
            )
            AppUserStatusFinderRequest request
    );

    @Operation(
            summary = "Start identifier sign-up",
            description = "Creates a new account in CREATED status and starts the verification flow for the supplied email address or phone number."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Sign-up started successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "signupCreated",
                                    value = """
                                            {
                                              "api_response_status": "success",
                                              "data": {
                                                "exists": true,
                                                "providers": ["LOCAL"],
                                                "email_verified": false,
                                                "app_user_status": "CREATED"
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "409", description = "Identifier already exists for another account")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<AppUserStatusFinderResponse>> signupUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identifier sign-up payload.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "signupRequest",
                                    value = """
                                            {
                                              "identifier": "intern.candidate@example.com",
                                              "identifier_type": "EMAIL",
                                              "password": "Intern@123"
                                            }
                                            """
                            )
                    )
            )
            AppUserSignUpRequest request
    );

    @Operation(
            summary = "Complete sign-up",
            description = "Marks a previously verified account as ready for normal sign-in after the OTP step has already succeeded."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Sign-up completed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "signupCompleted",
                                    value = """
                                            {
                                              "api_response_status": "success",
                                              "data": {
                                                "exists": true,
                                                "providers": ["LOCAL"],
                                                "email_verified": true,
                                                "app_user_status": "ACTIVE"
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Identifier is invalid or verification is not complete")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<AppUserStatusFinderResponse>> completeUserSignup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identifier that already passed verification.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "completeSignupRequest",
                                    value = """
                                            {
                                              "identifier": "intern.candidate@example.com",
                                              "identifier_type": "EMAIL"
                                            }
                                            """
                            )
                    )
            )
            AppUserSignUpCompleteRequest request
    );

    @Operation(
            summary = "Log in with identifier",
            description = "Authenticates credentials and sets HTTP-only access and refresh token cookies for the supplied device."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "loginSuccess",
                                    value = """
                                            {
                                              "message": "log in successful",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Credentials are invalid or the account is not ready to log in"),
            @ApiResponse(responseCode = "423", description = "Account is blocked")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> loginUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Credentials and client device identifier.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "loginRequest",
                                    value = """
                                            {
                                              "identifier": "intern.candidate@example.com",
                                              "password": "Intern@123",
                                              "device_id": "web-chrome-macos"
                                            }
                                            """
                            )
                    )
            )
            AppUserLoginRequest request,
            @Parameter(hidden = true) HttpServletResponse httpServletResponse,
            @Parameter(hidden = true) HttpServletRequest httpServletRequest
    );

    @Operation(
            summary = "Refresh access token",
            description = "Uses the refresh token cookie and the device identifier to rotate tokens and issue a fresh session cookie pair."
    )
    @SecurityRequirement(name = "refreshTokenCookie")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tokens refreshed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "refreshSuccess",
                                    value = """
                                            {
                                              "message": "refresh token sent",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Refresh token is missing, expired, or invalid")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> refreshToken(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Device identifier bound to the refresh token session.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "refreshRequest",
                                    value = """
                                            {
                                              "device_id": "web-chrome-macos"
                                            }
                                            """
                            )
                    )
            )
            DeviceIdRequest deviceIdRequest
    );

    @Operation(
            summary = "Request password reset OTP",
            description = "Sends a password-reset OTP for a local account. The success response is intentionally generic so account existence is not disclosed."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset instructions queued",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "passwordResetOtpQueued",
                                    value = """
                                            {
                                              "message": "If an account exists, you’ll receive reset instructions.",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Request validation failed")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> requestPasswordResetOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identifier for the account that needs a password reset OTP.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "passwordResetOtpRequest",
                                    value = """
                                            {
                                              "identifier": "intern.candidate@example.com",
                                              "identifier_type": "EMAIL"
                                            }
                                            """
                            )
                    )
            )
            PasswordResetRequestOtpRequest request
    );

    @Operation(
            summary = "Reset password with OTP",
            description = "Validates the reset OTP, updates the password, and revokes the account's active sessions."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset completed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "passwordResetSuccess",
                                    value = """
                                            {
                                              "message": "password reset successful",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "OTP, identifier, or password payload is invalid"),
            @ApiResponse(responseCode = "404", description = "No local account matches the supplied identifier")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> resetPassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Identifier, verified OTP, and replacement password.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "passwordResetConfirmRequest",
                                    value = """
                                            {
                                              "identifier": "intern.candidate@example.com",
                                              "identifier_type": "EMAIL",
                                              "otp": "123456",
                                              "new_password": "Reset@123"
                                            }
                                            """
                            )
                    )
            )
            PasswordResetConfirmRequest request
    );

    @Operation(
            summary = "Log out current device",
            description = "Revokes the current device session associated with the refresh token cookie and clears authentication cookies."
    )
    @SecurityRequirement(name = "refreshTokenCookie")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "logoutSuccess",
                                    value = """
                                            {
                                              "message": "user logged out successfully",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Refresh token is missing or no matching session exists")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> logoutUser(
            @Parameter(hidden = true) HttpServletResponse response,
            @Parameter(hidden = true) HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Device identifier for the session being logged out.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "logoutRequest",
                                    value = """
                                            {
                                              "device_id": "web-chrome-macos"
                                            }
                                            """
                            )
                    )
            )
            DeviceIdRequest deviceIdRequest
    );

    @Operation(
            summary = "Change password",
            description = "Changes the password for the authenticated local account and revokes existing sessions after the update."
    )
    @SecurityRequirement(name = "accessTokenCookie")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "passwordChangeSuccess",
                                    value = """
                                            {
                                              "message": "password change successful",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Current password is wrong or the new password is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required"),
            @ApiResponse(responseCode = "403", description = "Password change is not allowed for this account")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> changePassword(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Current password and the new password to store.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "passwordChangeRequest",
                                    value = """
                                            {
                                              "old_password": "Intern@123",
                                              "new_password": "Updated@123"
                                            }
                                            """
                            )
                    )
            )
            PasswordChangeRequest request
    );
}
