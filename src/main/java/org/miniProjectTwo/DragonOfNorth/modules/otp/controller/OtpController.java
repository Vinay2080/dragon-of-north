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

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
public class OtpController implements OtpApi {
    private final OtpService otpService;

    @Override
    @PostMapping("/email/request")
    public ResponseEntity<ApiResponse<?>> requestEmailOtp(@RequestBody @Valid EmailOtpRequest request) {
        otpService.createEmailOtp(request.email(), request.otpPurpose());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP sent"));
    }

    @Override
    @PostMapping("/phone/request")
    public ResponseEntity<ApiResponse<?>> requestPhoneOtp(@RequestBody @Valid PhoneOtpRequest request) {
        otpService.createPhoneOtp(request.phone(), request.otpPurpose());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successMessage("OTP Sent"));
    }

    @Override
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<OtpVerificationStatus>> verifyEmailOtp(@RequestBody @Valid EmailVerifyRequest request) {
        OtpVerificationStatus otpVerificationStatus = otpService.verifyEmailOtp(request.email(), request.otp(), request.otpPurpose());
        return otpVerificationStatus.isSuccess()
                ? ResponseEntity.accepted().body(ApiResponse.success(otpVerificationStatus))
                : ResponseEntity.badRequest().body(ApiResponse.failed(otpVerificationStatus));
    }

    @Override
    @PostMapping("/phone/verify")
    public ResponseEntity<ApiResponse<OtpVerificationStatus>> verifyPhoneOtp(@RequestBody @Valid PhoneVerifyRequest request) {
        OtpVerificationStatus otpVerificationStatus = otpService.verifyPhoneOtp(request.phone(), request.otp(), request.otpPurpose());
        return otpVerificationStatus.isSuccess()
                ? ResponseEntity.accepted().body(ApiResponse.success(otpVerificationStatus))
                : ResponseEntity.badRequest().body(ApiResponse.failed(otpVerificationStatus));
    }
}
