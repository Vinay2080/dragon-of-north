package org.miniProjectTwo.DragonOfNorth.enums;

import org.miniProjectTwo.DragonOfNorth.dto.otp.request.EmailOtpRequest;
import org.miniProjectTwo.DragonOfNorth.impl.otp.OtpService;

/**
 * OTP generation contexts controlling verification flow and security policies.
 * <p>
 * Purpose determines OTP expiration, rate limits, and verification behavior.
 * SIGNUP for account creation, LOGIN for authentication, PASSWORD_RESET for
 * recovery, TWO_FACTOR_AUTH for enhanced security. Critical for OtpService
 * routing and security rule enforcement.
 *
 * @see OtpService for purpose-based processing
 * @see EmailOtpRequest and PhoneOtpRequest for purpose usage
 */
public enum OtpPurpose {
    SIGNUP,
    LOGIN,
    PASSWORD_RESET,
    TWO_FACTOR_AUTH
}
