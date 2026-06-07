package org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.redis;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.config.MfaChallengeProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.ChallengeState;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Persistence adapter for MFA challenges state serialization and TTL management in Redis.
 * <p>
 * This component abstracts the Redis interactions for storing and retrieving MFA challenge states, including serialization/deserialization logic and TTL management. It provides methods to save a challenge state with a specified TTL, retrieve a challenge state by token ID, and delete challenge states. The TTL ensures that challenge states are automatically removed after a certain period, which is critical for security and resource management. The component relies on a codec for converting between ChallengeState objects and their string representations for Redis storage, and it uses properties defined in MfaChallengeProperties for default TTL values. Proper error handling is implemented to ensure that invalid TTL values are not accepted, and optional return types are used to handle cases where a challenge state may not be found in Redis.
 */
@Component
@RequiredArgsConstructor
public class ChallengeStateRedisStore {
    private final StringRedisTemplate redisTemplate;
    private final ChallengeStateCodec codec;
    private final MfaChallengeProperties properties;

    public void save(String tokenId, ChallengeState state, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        redisTemplate.opsForValue().set(MfaChallengeRedisKeys.challengeKey(tokenId), codec.serialize(state), ttl);
    }

    public void save(String tokenId, ChallengeState state) {
        save(tokenId, state, properties.getTtl());
    }

    public Optional<ChallengeState> find(String tokenId) {
        String value = redisTemplate.opsForValue().get(MfaChallengeRedisKeys.challengeKey(tokenId));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(codec.deserialize(value));
    }

    public void delete(String tokenId) {
        redisTemplate.delete(MfaChallengeRedisKeys.challengeKey(tokenId));
    }

    public boolean deleteIfPresent(String tokenId) {
        Boolean deleted = redisTemplate.delete(MfaChallengeRedisKeys.challengeKey(tokenId));
        return Boolean.TRUE.equals(deleted);
    }

    public ChallengeState decode(String payload) {
        return codec.deserialize(payload);
    }
}
