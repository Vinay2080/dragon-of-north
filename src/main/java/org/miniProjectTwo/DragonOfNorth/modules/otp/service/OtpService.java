package org.miniProjectTwo.DragonOfNorth.modules.otp.service;

import org.miniProjectTwo.DragonOfNorth.modules.otp.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpVerificationStatus;

import java.util.function.Function;

public interface OtpService {

    void createEmailOtp(String email, OtpPurpose otpPurpose);

    void createPhoneOtp(String phone, OtpPurpose otpPurpose);

    void createOtp(OtpSender sender, OtpPurpose otpPurpose,
                   String identifier, IdentifierType otpType, Function<String, String> normalizer);

    OtpVerificationStatus verifyEmailOtp(String email, String providedOtp, OtpPurpose otpPurpose);

    OtpVerificationStatus verifyPhoneOtp(String phone, String providedOtp, OtpPurpose otpPurpose);

    OtpToken fetchLatest(String identifier, IdentifierType otpType, OtpPurpose otpPurpose);

    OtpVerificationStatus verifyToken(OtpToken otpToken, String providedOtp, OtpPurpose otpPurpose);

    String generateOtp();

    void enforceRateLimits(String identifier, IdentifierType otpType, OtpPurpose otpPurpose);
}
