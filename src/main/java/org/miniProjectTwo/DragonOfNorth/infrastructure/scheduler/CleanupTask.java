package org.miniProjectTwo.DragonOfNorth.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.modules.otp.repo.OtpTokenRepository;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Runs scheduled cleanup jobs for OTP tokens, stale users, and sessions.
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class CleanupTask {
    private final OtpTokenRepository otpTokenRepository;
    private final AppUserRepository appUserRepository;
    private final SessionRepository sessionRepository;


    @Value("${session.cleanup.revoked-retention-days}")
    private long revokedRetentionDays;

    /**
     * Deletes OTP records whose expiration time has passed.
     */
    @Scheduled(fixedDelayString = "${otp.cleanup.delay-ms}")
    @Transactional
    public void cleanupExpiredOtpTokens() {
        try {
            otpTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
            log.debug("Expired OTP cleanup executed");
        } catch (RuntimeException exception) {
            log.error("Expired OTP cleanup failed", exception);
            throw exception;
        }
    }


    /**
     * Deletes unverified users that are older than the configured grace period.
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void cleanupUnverifiedUsers() {
        try {
            long users = appUserRepository.deleteByIsEmailVerifiedFalseAndCreatedAtBefore(Instant.now().minus(30, ChronoUnit.MINUTES));
            if (users > 0) {
                log.info("Deleted {} unverified users during scheduled cleanup", users);
            } else {
                log.debug("Unverified-user cleanup executed with no deletions");
            }
        } catch (RuntimeException exception) {
            log.error("Unverified-user cleanup failed", exception);
            throw exception;
        }
    }

    /**
     * Removes expired sessions and old revoked sessions.
     */
    @Scheduled(fixedDelayString = "${session.cleanup.delay-ms}")
    @Transactional
    public void cleanupSessions() {
        try {
            Instant now = Instant.now();
            long expiredDeleted = sessionRepository.deleteByExpiryDateBefore(now);

            Instant revokedCutoff = now.minus(revokedRetentionDays, ChronoUnit.DAYS);
            long revokedDeleted = sessionRepository.deleteByRevokedTrueAndUpdatedAtBefore(revokedCutoff);
            if (expiredDeleted > 0 || revokedDeleted > 0) {
                log.info("Session cleanup deleted expired={} revoked={}", expiredDeleted, revokedDeleted);
            } else {
                log.debug("Session cleanup executed with no deletions");
            }
        } catch (RuntimeException exception) {
            log.error("Session cleanup failed", exception);
            throw exception;
        }
    }

}
