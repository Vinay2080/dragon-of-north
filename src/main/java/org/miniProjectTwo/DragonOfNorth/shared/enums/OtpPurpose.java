package org.miniProjectTwo.DragonOfNorth.shared.enums;

import org.miniProjectTwo.DragonOfNorth.modules.otp.dto.request.EmailOtpRequest;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.impl.OtpServiceImpl;

/**
 * OTP generation contexts controlling verification flow and security policies.
 * <p>
 * Purpose determines OTP expiration, rate limits, and verification behavior.
 * SIGNUP for account creation, LOGIN for authentication, PASSWORD_RESET for
 * recovery, TWO_FACTOR_AUTH for enhanced security. Critical for OtpService
 * routing and security rule enforcement.
 *
 * @see OtpServiceImpl for purpose-based processing
 * @see EmailOtpRequest and PhoneOtpRequest for purpose usage
 */
public enum OtpPurpose {
    SIGNUP,
    LOGIN,
    LOGIN_UNVERIFIED,
    PASSWORD_RESET,
    TWO_FACTOR_AUTH
}
