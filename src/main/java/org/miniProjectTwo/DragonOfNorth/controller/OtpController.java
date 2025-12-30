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

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController {
    private final OtpService otpService;

    @PostMapping("/email/request")
    public ResponseEntity<ApiResponse<?>> requestEmailOtp(
            @RequestBody @Valid EmailOtpRequest request) {
        otpService.createEmailOtp(request.email(), request.otpPurpose());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP sent"));
    }

    @PostMapping("/phone/request")
    public ResponseEntity<ApiResponse<?>> requestPhoneOtp(
            @RequestBody
            @Valid
            PhoneOtpRequest request) {
        otpService.createPhoneOtp(request.phone(), request.otpPurpose());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP Sent"));
    }

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
