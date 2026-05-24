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
 **/

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * Core OTP orchestration implementation coordinating generation, sender dispatch, storage, and
 * verification outcomes with expiration/replay protections.
 */
public class OtpServiceImpl implements OtpService {
    private final OtpTokenRepository otpTokenRepository;
    private final EmailOtpSender emailOtpSender;
    private final PhoneOtpSender phoneOtpSender;
    private final MeterRegistry meterRegistry;
    private final AuditEventLogger auditEventLogger;

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
     **/

    @Transactional
    @Override
    public void createEmailOtp(String email, OtpPurpose otpPurpose) {
        createOtp(emailOtpSender, otpPurpose, email, EMAIL, IdentifierNormalizer::normalizeEmail);
    }

    /**
     * Generates and sends an OTP to a phone identifier.
     **/
    @Transactional
    @Override
    public void createPhoneOtp(String phone, OtpPurpose otpPurpose) {
        createOtp(phoneOtpSender, otpPurpose, phone, PHONE, IdentifierNormalizer::normalizePhone);
    }

    /**
     * Shared pipeline for normalized OTP creation and channel dispatch.
     **/
    @Override
    public void createOtp(OtpSender sender, OtpPurpose otpPurpose,
                          String identifier, IdentifierType otpType, Function<String, String> normalizer) {
        String normalizedIdentifier = normalizer.apply(identifier);
        try {
            issueOtp(sender, otpPurpose, normalizedIdentifier, otpType);
            recordOtpRequestSuccess(otpType, otpPurpose);
        } catch (RuntimeException ex) {
            recordOtpRequestFailure(ex);
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
     **/

    @Transactional
    @Override
    public OtpVerificationStatus verifyPhoneOtp(String phone, String providedOtp, OtpPurpose otpPurpose) {
        return verifyToken(fetchLatest(IdentifierNormalizer.normalizePhone(phone), PHONE, otpPurpose), providedOtp, otpPurpose);
    }

    /**
     * Fetches the latest token for identifier/type/purpose.
     **/

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
     **/

    @Override
    public OtpVerificationStatus verifyToken(OtpToken otpToken, String providedOtp, OtpPurpose otpPurpose) {
        OtpVerificationStatus failureStatus = validateTokenForVerification(otpToken, otpPurpose);
        if (failureStatus != null) {
            return failureStatus;
        }

        otpToken.incrementAttempts();

        boolean correct = BCrypt.checkpw(providedOtp, otpToken.getOtpHash());

        if (!correct) {
            return handleInvalidOtp(otpToken);
        }

        return handleSuccessfulVerification(otpToken);
    }

    /**
     * Generates a random numeric OTP of configured length.
     **/

    @Override
    public String generateOtp() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        return String.valueOf(secureRandom.nextInt(max - min + 1) + min);
    }

    /**
     * Enforces cooldown and request-window limits before issuing OTP.
     **/

    @Override
    public void enforceRateLimits(String identifier, IdentifierType otpType, OtpPurpose otpPurpose) {
        Instant now = Instant.now();
        checkCooldown(identifier, otpType, otpPurpose, now);
        checkRequestWindow(identifier, otpType, otpPurpose, now);
    }

    private OtpVerificationStatus validateTokenForVerification(OtpToken otpToken, OtpPurpose otpPurpose) {
        if (otpToken.isExpired()) {
            recordVerificationFailure(EXPIRED_OTP);
            return EXPIRED_OTP;
        }

        if (otpToken.getAttempts() >= maxAttempts) {
            recordVerificationFailure(MAX_ATTEMPT_EXCEEDED);
            return MAX_ATTEMPT_EXCEEDED;
        }
        if (otpToken.isConsumed()) {
            recordVerificationFailure(ALREADY_USED);
            return ALREADY_USED;
        }

        if (otpToken.getOtpPurpose() != otpPurpose) {
            recordVerificationFailure(INVALID_PURPOSE);
            return INVALID_PURPOSE;
        }

        return null;
    }

    private OtpVerificationStatus handleInvalidOtp(OtpToken otpToken) {
        otpTokenRepository.save(otpToken);
        recordVerificationFailure(INVALID_OTP);
        return INVALID_OTP;
    }

    private OtpVerificationStatus handleSuccessfulVerification(OtpToken otpToken) {
        otpToken.markVerified();
        otpTokenRepository.save(otpToken);
        meterRegistry.counter("auth.otp.verify.success").increment();
        auditEventLogger.log("auth.otp.verify", null, null, null, "success",
                "identifier_type=" + otpToken.getType() + ",purpose=" + otpToken.getOtpPurpose(), null);
        return SUCCESS;
    }

    private void recordVerificationFailure(OtpVerificationStatus status) {
        meterRegistry.counter("auth.otp.verify.failure").increment();
        auditEventLogger.log("auth.otp.verify", null, null, null, "failure", status.getMessage(), null);
    }

    private void issueOtp(OtpSender sender, OtpPurpose otpPurpose, String normalizedIdentifier, IdentifierType otpType) {
        enforceRateLimits(normalizedIdentifier, otpType, otpPurpose);
        otpTokenRepository.invalidateActiveTokens(normalizedIdentifier, otpType, otpPurpose);

        String otp = generateOtp();
        String hash = BCrypt.hashpw(otp, BCrypt.gensalt());

        OtpToken otpToken = new OtpToken(normalizedIdentifier, otpType, hash, ttlMinutes, otpPurpose);
        otpTokenRepository.save(otpToken);
        sender.send(normalizedIdentifier, otp, ttlMinutes);
    }

    private void recordOtpRequestSuccess(IdentifierType otpType, OtpPurpose otpPurpose) {
        meterRegistry.counter("auth.otp.request.success").increment();
        auditEventLogger.log("auth.otp.request", null, null, null, "success",
                "identifier_type=" + otpType + ",purpose=" + otpPurpose, null);
    }

    private void recordOtpRequestFailure(RuntimeException ex) {
        meterRegistry.counter("auth.otp.request.failure").increment();
        auditEventLogger.log("auth.otp.request", null, null, null, "failure", resolveFailureReason(ex), null);
    }

    private void checkCooldown(String identifier, IdentifierType otpType, OtpPurpose otpPurpose, Instant now) {
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
    }

    private void checkRequestWindow(String identifier, IdentifierType otpType, OtpPurpose otpPurpose, Instant now) {
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
