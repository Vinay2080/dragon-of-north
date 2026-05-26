package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RecentMfaServiceImplTest {

    @InjectMocks
    private RecentMfaServiceImpl recentMfaService;

    private static final Duration MAX_AGE = Duration.ofMinutes(15);

    // ------------------------------------------------------------------ isRecentMfaSatisfied

    @Test
    void isRecentMfaSatisfied_returnsTrue_whenVerifiedAtIsWithinMaxAge() {
        Instant recentlyVerified = Instant.now().minusSeconds(60);
        assertTrue(recentMfaService.isRecentMfaSatisfied(recentlyVerified, MAX_AGE));
    }

    @Test
    void isRecentMfaSatisfied_returnsFalse_whenVerifiedAtExceedsMaxAge() {
        Instant staleVerification = Instant.now().minus(Duration.ofMinutes(16));
        assertFalse(recentMfaService.isRecentMfaSatisfied(staleVerification, MAX_AGE));
    }

    @Test
    void isRecentMfaSatisfied_returnsFalse_whenVerifiedAtIsNull() {
        assertFalse(recentMfaService.isRecentMfaSatisfied(null, MAX_AGE));
    }

    @Test
    void isRecentMfaSatisfied_returnsFalse_whenVerifiedAtIsExactlyAtBoundary() {
        // exactly at boundary: verifiedAt + maxAge == now → not after now → false
        Instant boundary = Instant.now().minus(MAX_AGE);
        assertFalse(recentMfaService.isRecentMfaSatisfied(boundary, MAX_AGE));
    }

    @Test
    void isRecentMfaSatisfied_returnsTrue_whenVerifiedAtIsJustBeforeBoundary() {
        Instant justBefore = Instant.now().minus(MAX_AGE).plusSeconds(1);
        assertTrue(recentMfaService.isRecentMfaSatisfied(justBefore, MAX_AGE));
    }

    // ------------------------------------------------------------------ requireRecentMfa

    @Test
    void requireRecentMfa_doesNotThrow_whenMfaIsFresh() {
        Instant recentlyVerified = Instant.now().minusSeconds(30);
        assertDoesNotThrow(() -> recentMfaService.requireRecentMfa(recentlyVerified, MAX_AGE));
    }

    @Test
    void requireRecentMfa_throwsBusinessException_whenMfaIsStale() {
        Instant staleVerification = Instant.now().minus(Duration.ofMinutes(20));
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> recentMfaService.requireRecentMfa(staleVerification, MAX_AGE)
        );
        assertEquals(ErrorCode.MFA_STEP_UP_REQUIRED, ex.getErrorCode());
    }

    @Test
    void requireRecentMfa_throwsBusinessException_whenMfaVerifiedAtIsNull() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> recentMfaService.requireRecentMfa(null, MAX_AGE)
        );
        assertEquals(ErrorCode.MFA_STEP_UP_REQUIRED, ex.getErrorCode());
    }

    @Test
    void requireRecentMfa_throwsBusinessException_whenVerifiedAtIsExactlyAtBoundary() {
        Instant boundary = Instant.now().minus(MAX_AGE);
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> recentMfaService.requireRecentMfa(boundary, MAX_AGE)
        );
        assertEquals(ErrorCode.MFA_STEP_UP_REQUIRED, ex.getErrorCode());
    }
}
