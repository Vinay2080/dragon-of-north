package org.miniProjectTwo.DragonOfNorth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.EmailOtpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.EmailVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.PhoneOtpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.PhoneVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.enums.OtpVerificationStatus;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.OtpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for OTP (One-Time Password) operations.
 */
@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
@Tag(name = "OTP", description = "OTP generation and verification endpoints for email and phone flows")
public class OtpController {
    private final OtpService otpService;

    @PostMapping("/email/request")
    @Operation(summary = "Request OTP on email", description = "Generates a purpose-scoped OTP and dispatches it to the provided email.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "OTP generated and sent"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "OTP rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<?>> requestEmailOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "email": "intern.candidate@example.com",
                              "otp_purpose": "SIGNUP"
                            }
                            """)))
            @RequestBody @Valid EmailOtpRequest request) {
        otpService.createEmailOtp(request.email(), request.otpPurpose());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP sent"));
    }

    @PostMapping("/phone/request")
    @Operation(summary = "Request OTP on phone", description = "Generates a purpose-scoped OTP and dispatches it via SMS.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "OTP generated and sent"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "OTP rate limit exceeded")
    })
    public ResponseEntity<ApiResponse<?>> requestPhoneOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "phone": "9876543210",
                              "otp_purpose": "LOGIN"
                            }
                            """)))
            @RequestBody
            @Valid
            PhoneOtpRequest request) {
        otpService.createPhoneOtp(request.phone(), request.otpPurpose());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP Sent"));
    }

    @PostMapping("/email/verify")
    @Operation(summary = "Verify email OTP", description = "Verifies OTP for an email + purpose combination and returns verification outcome.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "OTP verified"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "OTP invalid, expired, or mismatched purpose")
    })
    public ResponseEntity<ApiResponse<OtpVerificationStatus>> verifyEmailOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "email": "intern.candidate@example.com",
                              "otp": "123456",
                              "otp_purpose": "SIGNUP"
                            }
                            """)))
            @Valid
            @RequestBody
            EmailVerifyRequest request) {
        OtpVerificationStatus otpVerificationStatus = otpService.verifyEmailOtp(request.email(), request.otp(), request.otpPurpose());
        return otpVerificationStatus.isSuccess() ?
                ResponseEntity.accepted().body(ApiResponse.success(otpVerificationStatus)) :
                ResponseEntity.badRequest().body(ApiResponse.failed(otpVerificationStatus));
    }

    @PostMapping("/phone/verify")
    @Operation(summary = "Verify phone OTP", description = "Verifies OTP for a phone + purpose combination and returns verification outcome.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "OTP verified"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "OTP invalid, expired, or mismatched purpose")
    })
    public ResponseEntity<ApiResponse<OtpVerificationStatus>> verifyPhoneOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "phone": "9876543210",
                              "otp": "123456",
                              "otp_purpose": "LOGIN"
                            }
                            """)))
            @Valid
            @RequestBody
            PhoneVerifyRequest request) {
        OtpVerificationStatus otpVerificationStatus = otpService.verifyPhoneOtp(request.phone(), request.otp(), request.otpPurpose());
        return otpVerificationStatus.isSuccess() ?
                ResponseEntity.accepted().body(ApiResponse.success(otpVerificationStatus)) :
                ResponseEntity.badRequest().body(ApiResponse.failed(otpVerificationStatus));
    }
}
