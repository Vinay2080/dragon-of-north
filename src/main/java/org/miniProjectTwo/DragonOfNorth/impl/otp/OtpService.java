package org.miniProjectTwo.DragonOfNorth.impl.otp;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.enums.OtpType;
import org.miniProjectTwo.DragonOfNorth.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.repositories.OtpTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

import static org.miniProjectTwo.DragonOfNorth.enums.OtpType.EMAIL;
import static org.miniProjectTwo.DragonOfNorth.enums.OtpType.PHONE;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final OtpTokenRepository otpTokenRepository;
    private final EmailOtpSender emailOtpSender;
    private final PhoneOtpSender phoneOtpSender;

    @Value("${otp.length}")
    private int otpLength;

    @Value("${otp.ttl-minutes}")
    private int ttlMinutes;

    @Value("${otp.max-verify-attempts}")
    private int maxAttempts;

    @Value("${otp.request-window-seconds}")
    private int requestWindowSeconds;

    @Value("${otp.block-duration-minutes}")
    private int blockDurationMinutes;

    @Value("${otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    @Value("${otp.max-requests-per-window}")
    private int maxRequestsPerWindow;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void createEmailOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        enforceRateLimits(normalizedEmail, EMAIL);

        String otp = generateOtp();
        String hash = BCrypt.hashpw(otp, BCrypt.gensalt());

        OtpToken otpToken = new OtpToken(normalizedEmail, EMAIL, hash, ttlMinutes);

        otpTokenRepository.save(otpToken);

        emailOtpSender.send(normalizedEmail, otp, ttlMinutes);
    }

    @Transactional
    public void createPhoneOtp(String phone) {
        String normalizedPhone = phone.replace(" ", "");

        enforceRateLimits(normalizedPhone, PHONE);

        String otp = generateOtp();
        String hash = BCrypt.hashpw(otp, BCrypt.gensalt());

        OtpToken otpToken = new OtpToken(normalizedPhone, PHONE, hash, ttlMinutes);

        otpTokenRepository.save(otpToken);

        phoneOtpSender.send(phone, otp, ttlMinutes);

    }

    @Transactional
    public void verifyEmailOtp(String email, String providedOtp) {
        verifyToken(
                fetchLatest(email.trim().toLowerCase(), EMAIL),
                providedOtp
        );
    }

    @Transactional
    public void verifyPhoneOtp(String phone, String providedOtp) {
        verifyToken(
                fetchLatest(phone.replace(" ", ""), PHONE),
                providedOtp);
    }

    private OtpToken fetchLatest(String identifier, OtpType otpType) {
        return otpTokenRepository
                .findTopByIdentifierAndTypeOrderByCreatedAtDesc(identifier, otpType)
                .orElseThrow(() -> new IllegalArgumentException("OTP not found"));
    }

    private void verifyToken(OtpToken otpToken, String providedOtp) {
        if (otpToken.isExpired()) {
            throw new IllegalStateException("OTP expired");
        }

        if (otpToken.getAttempts() >= maxAttempts) {
            throw new IllegalStateException("Max attempts exceeded");
        }

        otpToken.incrementAttempts();

        boolean correct = BCrypt.checkpw(providedOtp, otpToken.getOtpHash());

        if (!correct) {
            otpTokenRepository.save(otpToken);
            throw new IllegalArgumentException("Invalid OTP");
        }

        otpToken.markVerified();
        otpTokenRepository.save(otpToken);
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        return String.valueOf(secureRandom.nextInt(max - min + 1) + min);
    }

    private void enforceRateLimits(String identifier, OtpType otpType) {
        Instant now = Instant.now();

        otpTokenRepository.findTopByIdentifierAndTypeOrderByCreatedAtDesc(identifier, otpType)
                .ifPresent(last -> {
                    long delta = now.getEpochSecond() - last.getLastSentAt().getEpochSecond();
                    if (delta < resendCooldownSeconds) {
                        throw new IllegalStateException("wait " + (resendCooldownSeconds - delta) + " seconds");
                    }
                });

        Instant windowStart = now.minusSeconds(requestWindowSeconds);
        int requestCount = otpTokenRepository.countByIdentifierAndTypeAndCreatedAtAfter(identifier, otpType, windowStart);

        if (requestCount >= maxRequestsPerWindow) {
            throw new IllegalStateException("Too many otp requests. Blocked for " + blockDurationMinutes + " minutes.");
        }

    }
}
