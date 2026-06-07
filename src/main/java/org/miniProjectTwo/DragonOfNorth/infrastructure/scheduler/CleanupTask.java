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
 * Scheduled maintenance job orchestrator for ephemeral security data lifecycle.
 * <p>
 * Cross-module dependencies:
 * OTP repository (challenge expiration), user repository (stale unverified signups), and session
 * repository (expired/revoked session retention). Retention changes can affect login UX, forensic
 * analysis windows, and compliance/audit expectations.
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
     * <p>Runs at a fixed delay defined by the {@code otp.cleanup.delay-ms} property.  If cleanup fails, the exception is logged and rethrown to trigger any configured retry or alerting mechanisms.</p>
     * @throws RuntimeException if OTP cleanup fails
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
     * <p>Runs every 15 minutes.  If cleanup fails, the exception is logged and rethrown to trigger any configured retry or alerting mechanisms.</p>
     * @throws RuntimeException if user cleanup fails
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
     * <p>Runs at a fixed delay defined by the {@code session.cleanup.delay-ms} property.  If cleanup fails, the exception is logged and rethrown to trigger any configured retry or alerting mechanisms.</p>
     * @throws RuntimeException if session cleanup fails
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
