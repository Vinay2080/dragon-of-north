package org.miniProjectTwo.DragonOfNorth.impl.otp;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.enums.OtpVerificationStatus;
import org.miniProjectTwo.DragonOfNorth.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.repositories.OtpTokenRepository;
import org.miniProjectTwo.DragonOfNorth.services.OtpSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.function.Function;

import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.EMAIL;
import static org.miniProjectTwo.DragonOfNorth.enums.IdentifierType.PHONE;
import static org.miniProjectTwo.DragonOfNorth.enums.OtpVerificationStatus.*;

/**
 * Service for managing OTP (One-Time Password) generation, validation, and delivery.
 * This service handles both email and phone-based OTPs with rate limiting and security features.
 *
 * <p>Key features include:
 * <ul>
 *   <li>OTP generation with configurable length</li>
 *   <li>Rate limiting and cooldown periods</li>
 *   <li>Automatic expiration of OTPs</li>
 *   <li>Maximum attempt restrictions</li>
 *   <li>Secure storage using BCrypt hashing</li>
 * </ul>
 *
 * @see OtpToken
 * @see EmailOtpSender
 * @see PhoneOtpSender
 */

@Service
@RequiredArgsConstructor
public class OtpService {
    private final OtpTokenRepository otpTokenRepository;
    private final OtpSender emailOtpSender;
    private final OtpSender phoneOtpSender;

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

    /**
     * Generates and sends an OTP to the specified email address.
     * The OTP will be valid for the configured TTL period.
     *
     * @param email The email address to send the OTP to
     * @throws IllegalStateException    if rate limits are exceeded
     * @throws IllegalArgumentException if the email is invalid
     */

    @Transactional
    public void createEmailOtp(String email, OtpPurpose otpPurpose) {
        createOtp(emailOtpSender, otpPurpose, email, EMAIL, e -> e.trim().toLowerCase());
    }

    /**
     * Generates and sends an OTP to the specified phone number.
     * The OTP will be valid for the configured TTL period.
     *
     * @param phone The phone number to send the OTP to
     * @throws IllegalStateException    if rate limits are exceeded
     * @throws IllegalArgumentException if the phone number is invalid
     */
    @Transactional
    public void createPhoneOtp(String phone, OtpPurpose otpPurpose) {
        createOtp(phoneOtpSender, otpPurpose, phone, PHONE, p -> p.replace(" ", ""));
    }

    /**
     * Internal method to handle OTP creation and sending.
     *
     * @param identifier The identifier (email/phone) to send OTP to
     * @param otpType    The type of OTP (EMAIL/PHONE)
     * @param normalizer Function to normalize the identifier
     * @param sender     The appropriate sender service
     */
    private void createOtp(OtpSender sender, OtpPurpose otpPurpose,
                           String identifier, IdentifierType otpType, Function<String, String> normalizer) {
        String normalizedIdentifier = normalizer.apply(identifier);
        enforceRateLimits(normalizedIdentifier, otpType, otpPurpose);

        String otp = generateOtp();
        String hash = BCrypt.hashpw(otp, BCrypt.gensalt());

        OtpToken otpToken = new OtpToken(normalizedIdentifier, otpType, hash, ttlMinutes, otpPurpose);
        otpTokenRepository.save(otpToken);
        sender.send(normalizedIdentifier, otp, ttlMinutes);
    }

    /**
     * Verifies an email OTP.
     *
     * @param email       The email address the OTP was sent to
     * @param providedOtp The OTP to verify
     * @throws IllegalStateException    if the OTP is expired or max attempts exceeded
     * @throws IllegalArgumentException if the OTP is invalid
     */

    @Transactional
    public OtpVerificationStatus verifyEmailOtp(String email, String providedOtp, OtpPurpose otpPurpose) {
        return verifyToken(fetchLatest(email.trim().toLowerCase(), EMAIL, otpPurpose), providedOtp, otpPurpose);
    }

    /**
     * Verifies a phone OTP.
     *
     * @param phone       The phone number the OTP was sent to
     * @param providedOtp The OTP to verify
     * @throws IllegalStateException    if the OTP is expired or max attempts exceeded
     * @throws IllegalArgumentException if the OTP is invalid
     */

    @Transactional
    public OtpVerificationStatus verifyPhoneOtp(String phone, String providedOtp, OtpPurpose otpPurpose) {
        return verifyToken(fetchLatest(phone.replace(" ", ""), PHONE, otpPurpose), providedOtp, otpPurpose);
    }

    /**
     * Fetches the most recent OTP token for the given identifier and type.
     *
     * @param identifier The email or phone number
     * @param otpType    The type of OTP (EMAIL or PHONE)
     * @return The most recent OTP token
     * @throws IllegalArgumentException if no OTP is found
     */

    private OtpToken fetchLatest(String identifier, IdentifierType otpType, OtpPurpose otpPurpose) {
        return otpTokenRepository
                .findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(identifier, otpType, otpPurpose)
                .orElseThrow(() -> new IllegalArgumentException("OTP not found"));
    }

    /**
     * Verifies an OTP token.
     *
     * @param otpToken    The OTP token to verify
     * @param providedOtp The OTP to verify against
     * @throws IllegalStateException    if the OTP is expired or max attempts exceeded
     * @throws IllegalArgumentException if the OTP is invalid
     */

    private OtpVerificationStatus verifyToken(OtpToken otpToken, String providedOtp, OtpPurpose otpPurpose) {
        if (otpToken.isExpired()) {
            return EXPIRED_OTP;
        }

        if (otpToken.getAttempts() >= maxAttempts) {
            return MAX_ATTEMPT_EXCEEDED;
        }
        if (otpToken.isConsumed()) {
            return ALREADY_USED;
        }

        if (otpToken.getOtpPurpose() != otpPurpose) {
            return INVALID_PURPOSE;
        }

        otpToken.incrementAttempts();

        boolean correct = BCrypt.checkpw(providedOtp, otpToken.getOtpHash());

        if (!correct) {
            otpTokenRepository.save(otpToken);
            return INVALID_OTP;
        }

        otpToken.markVerified();
        otpTokenRepository.save(otpToken);
        return SUCCESS;
    }

    /**
     * Generates a random numeric OTP of the configured length.
     *
     * @return The generated OTP as a string
     */

    private String generateOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        return String.valueOf(secureRandom.nextInt(max - min + 1) + min);
    }

    /**
     * Enforces rate limiting for OTP requests.
     *
     * @param identifier The email or phone number
     * @param otpType    The type of OTP (EMAIL or PHONE)
     * @throws IllegalStateException if rate limits are exceeded
     */

    private void enforceRateLimits(String identifier, IdentifierType otpType, OtpPurpose otpPurpose) {
        Instant now = Instant.now();

        otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(identifier, otpType, otpPurpose)
                .ifPresent(last -> {
                    long delta = now.getEpochSecond() - last.getLastSentAt().getEpochSecond();
                    if (delta < resendCooldownSeconds) {
                        throw new IllegalStateException("wait " + (resendCooldownSeconds - delta) + " seconds before requesting another OTP for " +
                                otpPurpose.toString().toLowerCase().replace("_", " "));
                    }
                });

        Instant windowStart = now.minusSeconds(requestWindowSeconds);
        int requestCount = otpTokenRepository.countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter(identifier, otpType, otpPurpose, windowStart);

        if (requestCount >= maxRequestsPerWindow) {
            throw new IllegalStateException("Too many otp requests. Blocked for " + blockDurationMinutes + " minutes.");
        }

    }
}
