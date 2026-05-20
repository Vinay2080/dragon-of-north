package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis;

import java.util.UUID;

/**
 * Centralizes Redis key naming for MFA challenges.
 *
 * <p>Keys are intentionally separate from MFA setup namespaces.</p>
 */
public final class MfaChallengeRedisKeys {
    private static final String CHALLENGE_PREFIX = "auth:mfa:challenge:";
    private static final String CHALLENGE_LOCK_SUFFIX = ":lock";
    private static final String LOCKOUT_PREFIX = "auth:mfa:lockout:";

    private MfaChallengeRedisKeys() {
    }

    public static String challengeKey(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("tokenId must not be blank");
        }
        return CHALLENGE_PREFIX + tokenId.trim();
    }

    public static String lockoutKey(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return LOCKOUT_PREFIX + userId;
    }

    public static String challengeLockKey(String tokenId) {
        return challengeKey(tokenId) + CHALLENGE_LOCK_SUFFIX;
    }
}
