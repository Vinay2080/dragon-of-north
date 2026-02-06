package org.miniProjectTwo.DragonOfNorth.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.OtpTokenRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A scheduled task that periodically cleans up expired OTP tokens from the repository.
 * This helps maintain database cleanliness by removing stale OTP records that are no longer valid.
 * The cleanup interval is configurable through the application properties.
 *
 * @see org.springframework.scheduling.annotation.Scheduled
 * @see OtpTokenRepository
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class CleanupTask {
    private final OtpTokenRepository otpTokenRepository;
    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;


    /**
     * Executes the cleanup of expired OTP tokens.
     * This method is scheduled to run at a fixed delay as configured in the application properties.
     * It removes all OTP tokens that have expired before the current time.
     * The delay between executions is configured by the property 'otp.cleanup.delay-ms'.
     *
     * @see org.springframework.scheduling.annotation.Scheduled
     */

    @Scheduled(fixedDelayString = "${otp.cleanup.delay-ms}")
    public void cleanupExpiredOtpTokens() {
        otpTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
        log.info("Cleaned up all expired OTPs");
    }


    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void cleanupUnverifiedUsers() {
        appUserRepository.deleteByAppUserStatusAndCreatedAtBefore(
                AppUserStatus.CREATED,
                Instant.now().minus(30, ChronoUnit.MINUTES)
        );
        log.info("Cleaned up all unverified users");
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void CleanupExpiredJwtToken() {
        Instant now = Instant.now();
        int deletedCount = refreshTokenRepository.deleteByExpiryDateBefore(now);
        log.info("Cleaned up {} expired refresh tokens", deletedCount);
    }
}
