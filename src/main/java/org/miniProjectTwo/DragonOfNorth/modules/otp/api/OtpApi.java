package org.miniProjectTwo.DragonOfNorth.modules.otp.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request.EmailOtpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request.EmailVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request.PhoneOtpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request.PhoneVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpVerificationStatus;
import org.springframework.http.ResponseEntity;

@Tag(name = "OTP", description = "OTP request and verification flows for email and phone identifiers.")
public interface OtpApi {

    @Operation(
            summary = "Request email OTP",
            description = "Generates a purpose-specific OTP and sends it to the supplied email address."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "OTP created and queued for delivery",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "emailOtpCreated",
                                    value = """
                                            {
                                              "message": "OTP sent",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "429", description = "OTP rate limit exceeded")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> requestEmailOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Email address and business purpose for the OTP.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "emailOtpRequest",
                                    value = """
                                            {
                                              "email": "intern.candidate@example.com",
                                              "otp_purpose": "SIGNUP"
                                            }
                                            """
                            )
                    )
            )
            EmailOtpRequest request
    );

    @Operation(
            summary = "Request phone OTP",
            description = "Generates a purpose-specific OTP and sends it to the supplied phone number."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "OTP created and queued for delivery",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "phoneOtpCreated",
                                    value = """
                                            {
                                              "message": "OTP Sent",
                                              "api_response_status": "success",
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "429", description = "OTP rate limit exceeded")
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<?>> requestPhoneOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Phone number and business purpose for the OTP.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "phoneOtpRequest",
                                    value = """
                                            {
                                              "phone": "9876543210",
                                              "otp_purpose": "LOGIN"
                                            }
                                            """
                            )
                    )
            )
            PhoneOtpRequest request
    );

    @Operation(
            summary = "Verify email OTP",
            description = "Validates an email OTP against the identifier and purpose used during OTP creation."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "OTP verified successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "emailOtpVerified",
                                    value = """
                                            {
                                              "api_response_status": "success",
                                              "data": {
                                                "success": true,
                                                "message": "Verification successful"
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "OTP is invalid, expired, or used for the wrong purpose",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "emailOtpFailed",
                                    value = """
                                            {
                                              "api_response_status": "failed",
                                              "data": {
                                                "success": false,
                                                "message": "Invalid OTP"
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<OtpVerificationStatus>> verifyEmailOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Email address, OTP, and business purpose.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "emailOtpVerifyRequest",
                                    value = """
                                            {
                                              "email": "intern.candidate@example.com",
                                              "otp": "123456",
                                              "otp_purpose": "SIGNUP"
                                            }
                                            """
                            )
                    )
            )
            EmailVerifyRequest request
    );

    @Operation(
            summary = "Verify phone OTP",
            description = "Validates a phone OTP against the identifier and purpose used during OTP creation."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "202",
                    description = "OTP verified successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "phoneOtpVerified",
                                    value = """
                                            {
                                              "api_response_status": "success",
                                              "data": {
                                                "success": true,
                                                "message": "Verification successful"
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "OTP is invalid, expired, or used for the wrong purpose",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "phoneOtpFailed",
                                    value = """
                                            {
                                              "api_response_status": "failed",
                                              "data": {
                                                "success": false,
                                                "message": "Invalid OTP"
                                              },
                                              "time": "2026-04-04T06:45:00Z"
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse<OtpVerificationStatus>> verifyPhoneOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Phone number, OTP, and business purpose.",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "phoneOtpVerifyRequest",
                                    value = """
                                            {
                                              "phone": "9876543210",
                                              "otp": "123456",
                                              "otp_purpose": "LOGIN"
                                            }
                                            """
                            )
                    )
            )
            PhoneVerifyRequest request
    );
}
