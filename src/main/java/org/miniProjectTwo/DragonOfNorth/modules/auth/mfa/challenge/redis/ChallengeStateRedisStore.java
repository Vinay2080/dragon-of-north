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
