package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup;

import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Default implementation of {@link RecentMfaService}.
 *
 * <p>Freshness decisions are derived from session {@code mfa_verified_at} timestamps.
 * JWT claims are treated as a convenience hint only; authoritative truth is always the
 * session row.</p>
 */
@Service
public class RecentMfaServiceImpl implements RecentMfaService {

    /**
     * {@inheritDoc}
     *
     * <p>The freshness check is purely time-based: {@code mfaVerifiedAt + maxAge > now}.
     * This intentionally avoids coupling the check to device identity or risk signals —
     * those can be layered on in future adaptive-auth policies without changing call sites.</p>
     */
    @Override
    public boolean isRecentMfaSatisfied(Instant mfaVerifiedAt, Duration maxAge) {
        if (mfaVerifiedAt == null) {
            return false;
        }
        Instant freshUntil = mfaVerifiedAt.plus(maxAge);
        return freshUntil.isAfter(Instant.now());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Throws {@link BusinessException} with {@link ErrorCode#MFA_STEP_UP_REQUIRED} so the
     * exception handler maps it to HTTP 403.  The 403 (rather than 401) signals to the client
     * that the user <em>is</em> authenticated but lacks the required step-up verification —
     * a deliberate distinction that avoids triggering a full re-login flow.</p>
     */
    @Override
    public void requireRecentMfa(Instant mfaVerifiedAt, Duration maxAge) {
        if (!isRecentMfaSatisfied(mfaVerifiedAt, maxAge)) {
            throw new BusinessException(ErrorCode.MFA_STEP_UP_REQUIRED);
        }
    }
}
