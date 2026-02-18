package org.miniProjectTwo.DragonOfNorth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.OtpTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled cleanup task for expired tokens and unverified users.
 * <p>
 * Removes expired OTP tokens, unverified CREATED users, and expired
 * refresh tokens to maintain database hygiene and security.
 * Runs on configurable schedules for automated maintenance.
 * Critical for system performance and data cleanup.
 *
 * @see OtpTokenRepository for OTP cleanup
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class CleanupTask {
    private final OtpTokenRepository otpTokenRepository;
    private final AppUserRepository appUserRepository;


    /**
     * Removes expired OTP tokens from a database.
     * <p>
     * Deletes all OTP tokens with expiration time before now.
     * Runs on a configurable fixed delay schedule.
     * Critical for OTP table maintenance and performance.
     */
    @Scheduled(fixedDelayString = "${otp.cleanup.delay-ms}")
    @Transactional
    public void cleanupExpiredOtpTokens() {
        otpTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
        log.info("Cleaned up all expired OTPs");
    }


    /**
     * Removes unverified users older than 30 minutes.
     * <p>
     * Deletes users with CREATED status older than 30 minutes.
     * Runs every 15 minutes to prevent abandoned registrations.
     * Critical for user data cleanup and storage optimization.
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void cleanupUnverifiedUsers() {
        appUserRepository.deleteByAppUserStatusAndCreatedAtBefore(
                AppUserStatus.CREATED,
                Instant.now().minus(30, ChronoUnit.MINUTES)
        );
        log.info("Cleaned up all unverified users");
    }

//todo cleanup sessions

}
