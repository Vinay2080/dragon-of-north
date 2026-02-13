package org.miniProjectTwo.DragonOfNorth.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.EmailOtpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.EmailVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.PhoneOtpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.PhoneVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.enums.OtpVerificationStatus;
import org.miniProjectTwo.DragonOfNorth.impl.otp.OtpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for OTP (One-Time Password) operations.
 * Provides endpoints for generating and verifying OTP codes for both email
 * and phone number authentication. Supports various OTP purposes like registration,
 * login, and account recovery through the OtpService.
 */
@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {
    private final OtpService otpService;

    /**
     * Requests an OTP to be sent to the specified email address.
     * Generates and sends a one-time password via email for the specified purpose
     * (registration, login, etc.). The OTP will have a limited validity period.
     *
     * @param request the email OTP request containing the email address and purpose
     * @return success message indicating OTP was sent
     */
    @PostMapping("/email/request")
    public ResponseEntity<ApiResponse<?>> requestEmailOtp(
            @RequestBody @Valid EmailOtpRequest request) {
        otpService.createEmailOtp(request.email(), request.otpPurpose());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP sent"));
    }

    /**
     * Requests an OTP to be sent to the specified phone number.
     * Generates and sends a one-time password via SMS for the specified purpose
     * (registration, login, etc.). The OTP will have a limited validity period.
     *
     * @param request the phone OTP request containing phone number and purpose
     * @return success message indicating OTP was sent
     */
    @PostMapping("/phone/request")
    public ResponseEntity<ApiResponse<?>> requestPhoneOtp(
            @RequestBody
            @Valid
            PhoneOtpRequest request) {
        otpService.createPhoneOtp(request.phone(), request.otpPurpose());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP Sent"));
    }

    /**
     * Verifies an OTP sent to an email address.
     * Validates the provided OTP code against the stored value for the email
     * and purpose. Returns success status if valid, otherwise returns failure
     * with appropriate error details.
     *
     * @param request the email verification request containing email and OTP
     * @return verification status with success/failure details
     */
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<OtpVerificationStatus>> verifyEmailOtp(
            @Valid
            @RequestBody
            EmailVerifyRequest request) {
        OtpVerificationStatus otpVerificationStatus = otpService.verifyEmailOtp(request.email(), request.otp(), request.otpPurpose());
        return otpVerificationStatus.isSuccess() ?
                ResponseEntity.accepted().body(ApiResponse.success(otpVerificationStatus)) :
                ResponseEntity.badRequest().body(ApiResponse.failed(otpVerificationStatus));
    }

    /**
     * Verifies an OTP sent to a phone number.
     * Validates the provided OTP code against the stored value for the phone
     * number and purpose. Returns success status if valid, otherwise returns
     * failure with appropriate error details.
     *
     * @param request the phone verification request containing phone number and OTP
     * @return verification status with success/failure details
     */
    @PostMapping("/phone/verify")
    public ResponseEntity<ApiResponse<OtpVerificationStatus>> verifyPhoneOtp(
            @Valid
            @RequestBody
            PhoneVerifyRequest request) {
        OtpVerificationStatus otpVerificationStatus = otpService.verifyPhoneOtp(request.phone(), request.otp(), request.otpPurpose());
        return otpVerificationStatus.isSuccess() ?
                ResponseEntity.accepted().body(ApiResponse.success(otpVerificationStatus)) :
                ResponseEntity.badRequest().body(ApiResponse.failed(otpVerificationStatus));
    }
}
