package org.miniProjectTwo.DragonOfNorth.modules.otp.service;

import org.miniProjectTwo.DragonOfNorth.modules.otp.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpVerificationStatus;

import java.util.function.Function;

/**
 * OTP application service for request, verification, and rate-limit enforcement.
 */
public interface OtpService {

    /**
     * Generates and sends an email OTP for the given purpose.
     */
    void createEmailOtp(String email, OtpPurpose otpPurpose);

    /**
     * Generates and sends a phone OTP for the given purpose.
     */
    void createPhoneOtp(String phone, OtpPurpose otpPurpose);

    /**
     * Shared OTP creation pipeline used by channel-specific methods.
     */
    void createOtp(OtpSender sender, OtpPurpose otpPurpose,
                   String identifier, IdentifierType otpType, Function<String, String> normalizer);

    /**
     * Verifies an email OTP.
     */
    OtpVerificationStatus verifyEmailOtp(String email, String providedOtp, OtpPurpose otpPurpose);

    /**
     * Verifies a phone OTP.
     */
    OtpVerificationStatus verifyPhoneOtp(String phone, String providedOtp, OtpPurpose otpPurpose);

    /**
     * Returns the latest OTP token for identifier/type/purpose.
     */
    OtpToken fetchLatest(String identifier, IdentifierType otpType, OtpPurpose otpPurpose);

    /**
     * Validates a provided OTP value against a stored token.
     */
    OtpVerificationStatus verifyToken(OtpToken otpToken, String providedOtp, OtpPurpose otpPurpose);

    /**
     * Generates a random numeric OTP string.
     */
    String generateOtp();

    /**
     * Enforces OTP request cooldown and request-window limits.
     */
    void enforceRateLimits(String identifier, IdentifierType otpType, OtpPurpose otpPurpose);
}
