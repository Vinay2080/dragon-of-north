package org.miniProjectTwo.DragonOfNorth.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.OtpTokenRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
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
    void cleanupUnverifiedUsers_shouldCallDeleteByAppUserStatusAndCreatedAtBefore() {
        // act
        cleanupTask.cleanupUnverifiedUsers();

        // verify
        verify(appUserRepository).deleteByAppUserStatusAndCreatedAtBefore(
                eq(AppUserStatus.CREATED),
                any(Instant.class)
        );
    }

}
