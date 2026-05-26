package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup;

import java.time.Duration;
import java.time.Instant;

/**
 * Centralised utility contract for "recent MFA" evaluation and step-up authorization.
 *
 * <h2>Architecture rationale</h2>
 * <p>A valid, authenticated session proves <em>identity</em>.  A recently-completed MFA
 * verification proves <em>possession of the second factor right now</em>.  This distinction
 * prevents a scenario where an attacker who hijacks a long-lived authenticated session can
 * immediately perform sensitive security operations — they must still satisfy the second
 * factor within the freshness window before the operation is allowed.</p>
 *
 * <h2>Trust boundaries</h2>
 * <ul>
 *   <li>The source of truth for "when MFA was last verified" is the {@code mfa_verified_at}
 *       column on the session row — <strong>not</strong> a cookie, local state, or a separate
 *       token.</li>
 *   <li>After a successful step-up verification the session row is updated atomically and a
 *       new access token is minted from that truthful state, so every downstream claim check
 *       immediately reflects the refresh.</li>
 *   <li>No "half-authenticated" states are introduced: the session either satisfies recent-MFA
 *       or it does not; the enforcement is a read-only check on the existing session truth.</li>
 * </ul>
 *
 * <h2>Extensibility</h2>
 * <p>Future adaptive-auth policies (risk scoring, trusted-device bypass, elevated-privilege
 * scopes) can be added by extending the check inputs without changing the call sites — callers
 * only know about {@code requireRecentMfa} and the {@link RecentMfaProperties} max-age.</p>
 */
public interface RecentMfaService {

    /**
     * Returns {@code true} when {@code mfaVerifiedAt} is non-null and falls within
     * the supplied {@code maxAge} relative to the current clock.
     *
     * <p>A {@code null} {@code mfaVerifiedAt} always returns {@code false}.</p>
     *
     * @param mfaVerifiedAt timestamp of the last completed MFA verification, may be {@code null}
     * @param maxAge        maximum allowed age; must be positive
     * @return whether recent MFA is satisfied
     */
    boolean isRecentMfaSatisfied(Instant mfaVerifiedAt, Duration maxAge);

    /**
     * Asserts that recent MFA is satisfied; throws a {@link org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException}
     * with {@link org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode#MFA_STEP_UP_REQUIRED} when it is not.
     *
     * <p>This is the canonical authorization guard for sensitive operations.
     * Callers inject it at the service or controller layer before the protected operation:</p>
     * <pre>{@code
     *   recentMfaService.requireRecentMfa(principal.mfaVerifiedAt(), properties.getMfaMaxAge());
     *   // perform sensitive action
     * }</pre>
     *
     * @param mfaVerifiedAt timestamp of the last completed MFA verification, may be {@code null}
     * @param maxAge        maximum allowed age; must be positive
     * @throws org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException if recent MFA is not satisfied
     */
    void requireRecentMfa(Instant mfaVerifiedAt, Duration maxAge);
}
