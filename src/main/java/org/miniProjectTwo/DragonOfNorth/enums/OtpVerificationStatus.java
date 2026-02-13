package org.miniProjectTwo.DragonOfNorth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.EmailVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.impl.otp.OtpService;

/**
 * OTP verification outcomes with a success flag and user-facing messages.
 * <p>
 * Controls verification flow continuation and user feedback. SUCCESS enables
 * authentication progress, failures trigger retry limits or account locks.
 * Critical for security enforcement and user experience management.
 *
 * @see OtpService for verification logic
 * @see EmailVerifyRequest and PhoneVerifyRequest for usage
 */
@Getter
@RequiredArgsConstructor
public enum OtpVerificationStatus {

    SUCCESS(true, "Verification successful"),
    INVALID_OTP(false, "Invalid OTP"),
    EXPIRED_OTP(false, "OTP has expired"),
    MAX_ATTEMPT_EXCEEDED(false, "Maximum verification  attempts exceeded"),
    ALREADY_USED(false, "OTP has already been used"),
    INVALID_PURPOSE(false, "OTP was not generated for this purpose");

    private final boolean success;
    private final String message;


}
