package org.miniProjectTwo.DragonOfNorth.modules.otp.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.modules.otp.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.modules.otp.repo.OtpTokenRepository;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpSender;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpService;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpVerificationStatus;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.IdentifierNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.function.Function;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.EMAIL;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType.PHONE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.OtpVerificationStatus.*;

/**
 * Coordinates OTP generation, verification, and rate limiting.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private final OtpTokenRepository otpTokenRepository;
    private final EmailOtpSender emailOtpSender;
    private final PhoneOtpSender phoneOtpSender;
    private final MeterRegistry meterRegistry;
    private final AuditEventLogger auditEventLogger;
    private final Environment environment;

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
     * Generates and sends an OTP to an email identifier.
     */

    @Transactional
    @Override
    public void createEmailOtp(String email, OtpPurpose otpPurpose) {
        createOtp(emailOtpSender, otpPurpose, email, EMAIL, IdentifierNormalizer::normalizeEmail);
    }

    /**
     * Generates and sends an OTP to a phone identifier.
     */
    @Transactional
    @Override
    public void createPhoneOtp(String phone, OtpPurpose otpPurpose) {
        createOtp(phoneOtpSender, otpPurpose, phone, PHONE, IdentifierNormalizer::normalizePhone);
    }

    /**
     * Shared pipeline for normalized OTP creation and channel dispatch.
     */
    @Override
    public void createOtp(OtpSender sender, OtpPurpose otpPurpose,
                          String identifier, IdentifierType otpType, Function<String, String> normalizer) {
        String normalizedIdentifier = normalizer.apply(identifier);
        try {
            enforceRateLimits(normalizedIdentifier, otpType, otpPurpose);

            otpTokenRepository.invalidateActiveTokens(normalizedIdentifier, otpType, otpPurpose);

            String otp = generateOtp();

            String hash = BCrypt.hashpw(otp, BCrypt.gensalt());

            OtpToken otpToken = new OtpToken(normalizedIdentifier, otpType, hash, ttlMinutes, otpPurpose);
            otpTokenRepository.save(otpToken);
            sender.send(normalizedIdentifier, otp, ttlMinutes);

            meterRegistry.counter("auth.otp.request.success").increment();
            auditEventLogger.log("auth.otp.request", null, null, null, "success",
                    "identifier_type=" + otpType + ",purpose=" + otpPurpose, null);
        } catch (RuntimeException ex) {
            meterRegistry.counter("auth.otp.request.failure").increment();
            auditEventLogger.log("auth.otp.request", null, null, null, "failure", resolveFailureReason(ex), null);
            throw ex;
        }
    }

    /**
     * Verifies an OTP for an email identifier.
     */

    @Transactional
    @Override
    public OtpVerificationStatus verifyEmailOtp(String email, String providedOtp, OtpPurpose otpPurpose) {
        return verifyToken(fetchLatest(IdentifierNormalizer.normalizeEmail(email), EMAIL, otpPurpose), providedOtp, otpPurpose);
    }

    /**
     * Verifies an OTP for a phone identifier.
     */

    @Transactional
    @Override
    public OtpVerificationStatus verifyPhoneOtp(String phone, String providedOtp, OtpPurpose otpPurpose) {
        return verifyToken(fetchLatest(IdentifierNormalizer.normalizePhone(phone), PHONE, otpPurpose), providedOtp, otpPurpose);
    }

    /**
     * Fetches the latest token for identifier/type/purpose.
     */

    @Override
    public OtpToken fetchLatest(String identifier, IdentifierType otpType, OtpPurpose otpPurpose) {
        return otpTokenRepository
                .findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(identifier, otpType, otpPurpose)
                .orElseThrow(() -> {
                    auditEventLogger.log("auth.otp.verify", null, null, null, "failure", "otp_not_found", null);
                    return new BusinessException(ErrorCode.OTP_NOT_FOUND);
                });
    }

    /**
     * Applies OTP verification rules for expiration, attempts, purpose, and hash match.
     */

    @Override
    public OtpVerificationStatus verifyToken(OtpToken otpToken, String providedOtp, OtpPurpose otpPurpose) {
        if (otpToken.isExpired()) {
            meterRegistry.counter("auth.otp.verify.failure").increment();
            auditEventLogger.log("auth.otp.verify", null, null, null, "failure", EXPIRED_OTP.getMessage(), null);
            return EXPIRED_OTP;
        }

        if (otpToken.getAttempts() >= maxAttempts) {
            meterRegistry.counter("auth.otp.verify.failure").increment();
            auditEventLogger.log("auth.otp.verify", null, null, null, "failure", MAX_ATTEMPT_EXCEEDED.getMessage(), null);
            return MAX_ATTEMPT_EXCEEDED;
        }
        if (otpToken.isConsumed()) {
            meterRegistry.counter("auth.otp.verify.failure").increment();
            auditEventLogger.log("auth.otp.verify", null, null, null, "failure", ALREADY_USED.getMessage(), null);
            return ALREADY_USED;
        }

        if (otpToken.getOtpPurpose() != otpPurpose) {
            meterRegistry.counter("auth.otp.verify.failure").increment();
            auditEventLogger.log("auth.otp.verify", null, null, null, "failure", INVALID_PURPOSE.getMessage(), null);
            return INVALID_PURPOSE;
        }

        otpToken.incrementAttempts();

        boolean correct = BCrypt.checkpw(providedOtp, otpToken.getOtpHash());

        if (!correct) {
            otpTokenRepository.save(otpToken);
            meterRegistry.counter("auth.otp.verify.failure").increment();
            auditEventLogger.log("auth.otp.verify", null, null, null, "failure", INVALID_OTP.getMessage(), null);
            return INVALID_OTP;
        }

        otpToken.markVerified();
        otpTokenRepository.save(otpToken);
        meterRegistry.counter("auth.otp.verify.success").increment();
        auditEventLogger.log("auth.otp.verify", null, null, null, "success",
                "identifier_type=" + otpToken.getType() + ",purpose=" + otpToken.getOtpPurpose(), null);
        return SUCCESS;
    }

    /**
     * Generates a random numeric OTP of configured length.
     */

    @Override
    public String generateOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        return String.valueOf(secureRandom.nextInt(max - min + 1) + min);
    }

    /**
     * Enforces cooldown and request-window limits before issuing OTP.
     */

    @Override
    public void enforceRateLimits(String identifier, IdentifierType otpType, OtpPurpose otpPurpose) {
        Instant now = Instant.now();

        otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(identifier, otpType, otpPurpose)
                .ifPresent(last -> {
                    long delta = now.getEpochSecond() - last.getLastSentAt().getEpochSecond();
                    if (delta < resendCooldownSeconds) {
                        meterRegistry.counter("auth.otp.request.failure").increment();
                        auditEventLogger.log("auth.otp.request", null, null, null, "failure", "cooldown_active", null);
                        throw new BusinessException(ErrorCode.OTP_RATE_LIMIT,
                                (resendCooldownSeconds - delta),
                                otpPurpose.toString().toLowerCase().replace("_", " "));
                    }
                });

        Instant windowStart = now.minusSeconds(requestWindowSeconds);
        int requestCount = otpTokenRepository.countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter(identifier, otpType, otpPurpose, windowStart);

        if (requestCount >= maxRequestsPerWindow) {
            meterRegistry.counter("auth.otp.request.failure").increment();
            auditEventLogger.log("auth.otp.request", null, null, null, "failure", "request_window_exceeded", null);
            throw new BusinessException(ErrorCode.OTP_TOO_MANY_REQUESTS, blockDurationMinutes);
        }

    }

    private String resolveFailureReason(RuntimeException exception) {
        if (exception instanceof BusinessException businessException) {
            return "business_" + businessException.getErrorCode().name().toLowerCase();
        }
        return "internal_error";
    }
}
