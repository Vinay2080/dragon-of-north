package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis;

import java.util.UUID;

/**
 * Centralizes Redis key naming for MFA challenges.
 * <ol>
 *     <li>{@code auth:mfa:challenge:{tokenId}} - Stores the MFA challenge state for a specific tokenId, which represents an in-progress MFA authentication attempt. This key holds the necessary information to validate MFA codes, track attempts, and manage the challenge lifecycle.</li>
 *     <li>{@code auth:mfa:challenge:{tokenId}:lock} - A lock key used to prevent concurrent modifications to the same MFA challenge state, ensuring atomicity of operations like attempt increments and state updates.</li>
 *     <li>{@code auth:mfa:lockout:{userId}} - Tracks lockout status for a user after exceeding maximum MFA verification attempts, preventing further MFA challenges for that user until the lockout expires.</li>
 * </ol>
 * <p>Keys are intentionally separated from MFA setup namespaces.</p>
 */
public final class MfaChallengeRedisKeys {
    private static final String CHALLENGE_PREFIX = "auth:mfa:challenge:";
    private static final String CHALLENGE_LOCK_SUFFIX = ":lock";
    private static final String LOCKOUT_PREFIX = "auth:mfa:lockout:";

    private MfaChallengeRedisKeys() {
    }

    /**
     * Generates the Redis key for the lock associated with a specific MFA challenge tokenId.
     *
     * @param userId The unique identifier for the MFA challenge.
     * @return The Redis key in the format "auth:mfa:challenge:{tokenId}:lock".
     * @throws IllegalArgumentException if tokenId is null or blank.
     */
    public static String lockoutKey(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return LOCKOUT_PREFIX + userId;
    }

    /**
     * Generates the Redis key for the lock associated with a specific MFA challenge tokenId.
     *
     * @param tokenId The unique identifier for the MFA challenge, typically associated with an authentication attempt.
     * @return The Redis key in the format "auth:mfa:challenge:{tokenId}:lock".
     * @throws IllegalArgumentException if tokenId is null or blank.
     */
    public static String challengeLockKey(String tokenId) {
        return challengeKey(tokenId) + CHALLENGE_LOCK_SUFFIX;
    }

    /**
     * Generates the Redis key for storing MFA challenge state based on the provided tokenId.
     *
     * @param tokenId The unique identifier for the MFA challenge, typically associated with an authentication attempt.
     * @return The Redis key in the format "auth:mfa:challenge:{tokenId}".
     * @throws IllegalArgumentException if tokenId is null or blank.
     */
    public static String challengeKey(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("tokenId must not be blank");
        }
        return CHALLENGE_PREFIX + tokenId.trim();
    }
}
