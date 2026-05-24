package org.miniProjectTwo.DragonOfNorth.modules.otp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.otp.api.OtpApi;
import org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request.EmailOtpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request.EmailVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request.PhoneOtpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request.PhoneVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpVerificationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OTP controller that exposes request/verify endpoints for email and phone factors.
 * <p>
 * OTP flows are intentionally separated from authentication endpoints so sign-up, login,
 * and recovery paths can reuse the same verification primitives across modules.
 */
@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController implements OtpApi {
    private final OtpService otpService;

    /**
     * Issues an email OTP for the requested purpose (for example SIGNUP or PASSWORD_RESET).
     */
    @Override
    @PostMapping("/email/request")
    public ResponseEntity<ApiResponse<?>> requestEmailOtp(@RequestBody @Valid EmailOtpRequest request) {
        otpService.createEmailOtp(request.email(), request.otpPurpose());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP sent"));
    }

    /**
     * Issues a phone OTP for the requested purpose (for example LOGIN verification).
     */
    @Override
    @PostMapping("/phone/request")
    public ResponseEntity<ApiResponse<?>> requestPhoneOtp(@RequestBody @Valid PhoneOtpRequest request) {
        otpService.createPhoneOtp(request.phone(), request.otpPurpose());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP Sent"));
    }

    /**
     * Verifies an email OTP and returns status in both success and failed response envelopes.
     */
    @Override
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<OtpVerificationStatus>> verifyEmailOtp(@RequestBody @Valid EmailVerifyRequest request) {
        OtpVerificationStatus otpVerificationStatus = otpService.verifyEmailOtp(request.email(), request.otp(), request.otpPurpose());
        return otpVerificationStatus.isSuccess()
                ? ResponseEntity.accepted().body(ApiResponse.success(otpVerificationStatus))
                : ResponseEntity.badRequest().body(ApiResponse.failed(otpVerificationStatus));
    }

    /**
     * Verifies a phone OTP and returns status in both success and failed response envelopes.
     */
    @Override
    @PostMapping("/phone/verify")
    public ResponseEntity<ApiResponse<OtpVerificationStatus>> verifyPhoneOtp(@RequestBody @Valid PhoneVerifyRequest request) {
        OtpVerificationStatus otpVerificationStatus = otpService.verifyPhoneOtp(request.phone(), request.otp(), request.otpPurpose());
        return otpVerificationStatus.isSuccess()
                ? ResponseEntity.accepted().body(ApiResponse.success(otpVerificationStatus))
                : ResponseEntity.badRequest().body(ApiResponse.failed(otpVerificationStatus));
    }
}
