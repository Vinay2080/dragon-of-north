package org.miniProjectTwo.DragonOfNorth.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
