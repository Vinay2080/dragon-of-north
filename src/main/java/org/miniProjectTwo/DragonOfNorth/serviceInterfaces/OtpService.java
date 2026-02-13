package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import jakarta.transaction.Transactional;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.enums.OtpVerificationStatus;
import org.miniProjectTwo.DragonOfNorth.model.OtpToken;

import java.util.function.Function;

public interface OtpService {

    @Transactional
    void createEmailOtp(String email, OtpPurpose otpPurpose);

    @Transactional
    void createPhoneOtp(String phone, OtpPurpose otpPurpose);

    void createOtp(OtpSender sender, OtpPurpose otpPurpose,
                   String identifier, IdentifierType otpType, Function<String, String> normalizer);

    @Transactional
    OtpVerificationStatus verifyEmailOtp(String email, String providedOtp, OtpPurpose otpPurpose);

    @Transactional
    OtpVerificationStatus verifyPhoneOtp(String phone, String providedOtp, OtpPurpose otpPurpose);

    OtpToken fetchLatest(String identifier, IdentifierType otpType, OtpPurpose otpPurpose);

    OtpVerificationStatus verifyToken(OtpToken otpToken, String providedOtp, OtpPurpose otpPurpose);

    String generateOtp();

    void enforceRateLimits(String identifier, IdentifierType otpType, OtpPurpose otpPurpose);
}
