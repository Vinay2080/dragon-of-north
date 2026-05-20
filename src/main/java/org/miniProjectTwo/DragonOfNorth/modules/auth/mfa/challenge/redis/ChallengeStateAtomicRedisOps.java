package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Atomic Redis operations for challenge verification/consumption.
 *
 * <p>Implements replay protection and safe attempt mutation using Lua scripts.</p>
 */
@Component
@RequiredArgsConstructor
public class ChallengeStateAtomicRedisOps {
    private static final String LOCKOUT_PREFIX = "auth:mfa:lockout:";

    private static final DefaultRedisScript<List> CLAIM_SCRIPT = new DefaultRedisScript<>("""
            local challengeKey = KEYS[1]
            local lockKey = KEYS[2]
            local lockValue = ARGV[1]
            local lockMs = tonumber(ARGV[2])
            local lockoutPrefix = ARGV[3]

            local stateJson = redis.call('GET', challengeKey)
            if not stateJson then
              return {'missing'}
            end

            local state = cjson.decode(stateJson)
            local userId = state['user_id']
            if userId and redis.call('EXISTS', lockoutPrefix .. userId) == 1 then
              return {'locked_out'}
            end

            if redis.call('SET', lockKey, lockValue, 'NX', 'PX', lockMs) == false then
              return {'busy'}
            end

            return {'ok', stateJson}
            """);

    private static final DefaultRedisScript<List> FAIL_SCRIPT = new DefaultRedisScript<>("""
            local challengeKey = KEYS[1]
            local lockKey = KEYS[2]
            local lockValue = ARGV[1]
            local maxAttempts = tonumber(ARGV[2])
            local lockoutMs = tonumber(ARGV[3])
            local lockoutPrefix = ARGV[4]

            local currentLock = redis.call('GET', lockKey)
            if not currentLock then
              return {'not_owner'}
            end
            if currentLock ~= lockValue then
              return {'not_owner'}
            end

            local stateJson = redis.call('GET', challengeKey)
            if not stateJson then
              redis.call('DEL', lockKey)
              return {'missing'}
            end

            local state = cjson.decode(stateJson)
            local attempts = tonumber(state['attempts']) or 0
            attempts = attempts + 1
            state['attempts'] = attempts

            local userId = state['user_id']
            if attempts >= maxAttempts then
              redis.call('DEL', challengeKey)
              redis.call('DEL', lockKey)
              if userId then
                redis.call('SET', lockoutPrefix .. userId, '1', 'PX', lockoutMs)
              end
              return {'locked', tostring(attempts), userId}
            end

            redis.call('SET', challengeKey, cjson.encode(state), 'KEEPTTL')
            redis.call('DEL', lockKey)
            return {'failed', tostring(attempts), userId}
            """);

    private static final DefaultRedisScript<Long> SUCCESS_SCRIPT = new DefaultRedisScript<>("""
            local challengeKey = KEYS[1]
            local lockKey = KEYS[2]
            local lockValue = ARGV[1]

            local currentLock = redis.call('GET', lockKey)
            if not currentLock or currentLock ~= lockValue then
              return 0
            end

            redis.call('DEL', challengeKey)
            redis.call('DEL', lockKey)
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public ClaimResult claim(String tokenId, String lockValue, Duration lockTtl) {
        Objects.requireNonNull(lockTtl, "lockTtl must not be null");
        if (lockTtl.isNegative() || lockTtl.isZero()) {
            throw new IllegalArgumentException("lockTtl must be positive");
        }

        List<?> result = redisTemplate.execute(
                CLAIM_SCRIPT,
                List.of(
                        MfaChallengeRedisKeys.challengeKey(tokenId),
                        MfaChallengeRedisKeys.challengeLockKey(tokenId)
                ),
                lockValue,
                String.valueOf(lockTtl.toMillis()),
                LOCKOUT_PREFIX
        );

        if (result == null || result.isEmpty()) {
            return new ClaimResult(ClaimStatus.MISSING, null);
        }

        String status = String.valueOf(result.getFirst());
        return switch (status) {
            case "ok" -> new ClaimResult(ClaimStatus.OK, String.valueOf(result.get(1)));
            case "busy" -> new ClaimResult(ClaimStatus.BUSY, null);
            case "locked_out" -> new ClaimResult(ClaimStatus.LOCKED_OUT, null);
            default -> new ClaimResult(ClaimStatus.MISSING, null);
        };
    }

    public FailResult recordFailure(String tokenId, String lockValue, int maxAttempts, Duration lockoutTtl) {
        Objects.requireNonNull(lockoutTtl, "lockoutTtl must not be null");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }

        List<?> result = redisTemplate.execute(
                FAIL_SCRIPT,
                List.of(
                        MfaChallengeRedisKeys.challengeKey(tokenId),
                        MfaChallengeRedisKeys.challengeLockKey(tokenId)
                ),
                lockValue,
                String.valueOf(maxAttempts),
                String.valueOf(lockoutTtl.toMillis()),
                LOCKOUT_PREFIX
        );

        if (result == null || result.isEmpty()) {
            return new FailResult(FailStatus.MISSING, null, null);
        }

        String status = String.valueOf(result.getFirst());
        if ("not_owner".equals(status)) {
            return new FailResult(FailStatus.NOT_OWNER, null, null);
        }
        if ("missing".equals(status)) {
            return new FailResult(FailStatus.MISSING, null, null);
        }

        Integer attempts = result.size() > 1 ? Integer.valueOf(String.valueOf(result.get(1))) : null;
        String userId = result.size() > 2 ? (result.get(2) == null ? null : String.valueOf(result.get(2))) : null;
        return switch (status) {
            case "locked" -> new FailResult(FailStatus.LOCKED, attempts, userId);
            case "failed" -> new FailResult(FailStatus.FAILED, attempts, userId);
            default -> new FailResult(FailStatus.MISSING, null, null);
        };
    }

    public boolean consumeSuccess(String tokenId, String lockValue) {
        Long result = redisTemplate.execute(
                SUCCESS_SCRIPT,
                List.of(
                        MfaChallengeRedisKeys.challengeKey(tokenId),
                        MfaChallengeRedisKeys.challengeLockKey(tokenId)
                ),
                lockValue
        );
        return Objects.equals(result, 1L);
    }

    public record ClaimResult(ClaimStatus status, String stateJson) {
    }

    public enum ClaimStatus {
        OK,
        MISSING,
        BUSY,
        LOCKED_OUT
    }

    public record FailResult(FailStatus status, Integer attempts, String userId) {
    }

    public enum FailStatus {
        FAILED,
        LOCKED,
        MISSING,
        NOT_OWNER
    }
}

