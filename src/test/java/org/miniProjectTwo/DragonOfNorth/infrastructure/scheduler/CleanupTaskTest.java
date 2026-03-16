package org.miniProjectTwo.DragonOfNorth.infrastructure.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.infrastructure.scheduler.CleanupTask;
import org.miniProjectTwo.DragonOfNorth.modules.otp.repo.OtpTokenRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CleanupTaskTest {

    @InjectMocks
    private CleanupTask cleanupTask;

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Test
    void cleanupExpired_Otps_shouldCallDeleteAllByExpiresAtBefore() {
        // act
        cleanupTask.cleanupExpiredOtpTokens();

        // verify
        verify(otpTokenRepository).deleteAllByExpiresAtBefore(any(Instant.class));
    }

    @Test
    void cleanupUnverifiedUsers_shouldCallDeleteByIsEmailVerifiedFalseAndCreatedAtBefore() {
        // act
        cleanupTask.cleanupUnverifiedUsers();

        // verify
        verify(appUserRepository).deleteByIsEmailVerifiedFalseAndCreatedAtBefore(any(Instant.class));
    }
}
