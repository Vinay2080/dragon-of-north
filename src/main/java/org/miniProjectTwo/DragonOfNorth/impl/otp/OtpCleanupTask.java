
package org.miniProjectTwo.DragonOfNorth.impl.otp;

import org.miniProjectTwo.DragonOfNorth.repositories.OtpTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * A scheduled task that periodically cleans up expired OTP tokens from the repository.
 * This helps maintain database cleanliness by removing stale OTP records that are no longer valid.
 * The cleanup interval is configurable through the application properties.
 *
 * @see org.springframework.scheduling.annotation.Scheduled
 * @see OtpTokenRepository
 */

@Component
public class OtpCleanupTask {
    private final OtpTokenRepository repo;

    /**
     * Constructs a new OtpCleanupTask with the specified repository.
     *
     * @param repo The repository used to access OTP tokens
     */
    public OtpCleanupTask(OtpTokenRepository repo) {
        this.repo = repo;
    }

    /**
     * Executes the cleanup of expired OTP tokens.
     * This method is scheduled to run at a fixed delay as configured in the application properties.
     * It removes all OTP tokens that have expired before the current time.
     * The delay between executions is configured by the property 'otp.cleanup.delay-ms'.
     *
     * @see org.springframework.scheduling.annotation.Scheduled
     */

    @Scheduled(fixedDelayString = "${otp.cleanup.delay-ms}")
    public void cleanupExpired() {
        repo.deleteAllByExpiresAtBefore(Instant.now());
    }
}
