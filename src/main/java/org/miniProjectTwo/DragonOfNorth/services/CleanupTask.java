package org.miniProjectTwo.DragonOfNorth.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.OtpTokenRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled cleanup task for OTPs, stale users, and expired sessions.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class CleanupTask {
    private final OtpTokenRepository otpTokenRepository;
    private final AppUserRepository appUserRepository;
    private final SessionRepository sessionRepository;

    @Value("${session.cleanup.revoked-retention-days:7}")
    private long revokedRetentionDays;

    @Scheduled(fixedDelayString = "${otp.cleanup.delay-ms}")
    @Transactional
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

    @Scheduled(fixedDelayString = "${session.cleanup.delay-ms:900000}")
    @Transactional
    public void cleanupSessions() {
        Instant now = Instant.now();
        long expiredDeleted = sessionRepository.deleteByExpiryDateBefore(now);

        Instant revokedCutoff = now.minus(revokedRetentionDays, ChronoUnit.DAYS);
        long revokedDeleted = sessionRepository.deleteByRevokedTrueAndUpdatedAtBefore(revokedCutoff);

        log.info("Session cleanup completed: expired_deleted={}, revoked_deleted={}", expiredDeleted, revokedDeleted);
    }
}
