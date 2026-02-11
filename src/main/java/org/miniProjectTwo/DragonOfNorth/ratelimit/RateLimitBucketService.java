package org.miniProjectTwo.DragonOfNorth.ratelimit;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.config.RateLimitProperties;
import org.miniProjectTwo.DragonOfNorth.enums.RateLimitType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Service
@Slf4j
public class RateLimitBucketService {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties properties;
    private final Map<RateLimitType, BucketConfiguration> configCache = new EnumMap<>(RateLimitType.class);

    public RateLimitBucketService(ProxyManager<String> proxyManager, RateLimitProperties properties) {
        this.proxyManager = proxyManager;
        this.properties = properties;
    }

    @PostConstruct
    public void initializeConfigurations() {
        for (RateLimitType type : RateLimitType.values()) {
            String ruleKey = type.name().toLowerCase();
            RateLimitProperties.LimitRule rule = properties.getRules().get(ruleKey);

            if (rule == null) {
                throw new IllegalStateException(
                        "Missing rate limit configuration for type: " + type +
                                ". Add 'rate-limit.rules." + ruleKey + "' to application.yml"
                );
            }
            configCache.put(type, createBucketConfiguration(type));
            log.info("Initialized rate limit config for {}: capacity={}, refill={} tokens/{} min",
                    type, rule.getCapacity(), rule.getRefillTokens(), rule.getRefillMinutes());

        }
    }

    public ConsumptionResult tryConsume(String key, RateLimitType type) {
        try {
            var bucket = proxyManager.builder().build(key, () -> configCache.get(type));
            var probe = bucket.tryConsumeAndReturnRemaining(1);

            long remainingTokens = probe.getRemainingTokens();
            long capacity = configCache.get(type).getBandwidths()[0].getCapacity();

            if (probe.isConsumed()) {
                return ConsumptionResult.allowed(remainingTokens, capacity);
            } else {
                long waitNanos = probe.getNanosToWaitForRefill();
                long retryAfterSeconds = Duration.ofNanos(waitNanos).getSeconds() + 1;

                return ConsumptionResult.blocked(remainingTokens, capacity, retryAfterSeconds);
            }
        } catch (Exception e) {
            log.error("Rate limit check failed for key={}, type={}", key, type, e);
            // FAIL-OPEN: Allow request if Redis is down (change to FAIL-CLOSED for strict security)
            // For production, consider: return ConsumptionResult.blocked(0, 0, 60);
            return ConsumptionResult.allowed(0, 0);
        }


    }

    private BucketConfiguration createBucketConfiguration(RateLimitType type) {
        RateLimitProperties.LimitRule rule = properties.getRules().get(type.name().toLowerCase());

        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(rule.getCapacity())
                        .refillGreedy(rule.getRefillTokens(), Duration.ofMinutes(rule.getRefillMinutes())))
                .build();
    }

    @Getter
    public static class ConsumptionResult {
        private final boolean allowed;
        private final long remaining;
        private final long capacity;
        private final long retryAfterSeconds;

        private ConsumptionResult(boolean allowed, long remaining, long capacity, long retryAfterSeconds) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.capacity = capacity;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public static ConsumptionResult allowed(long remaining, long capacity) {
            return new ConsumptionResult(true, remaining, capacity, 0);
        }

        public static ConsumptionResult blocked(long remaining, long capacity, long retryAfterSeconds) {
            return new ConsumptionResult(false, remaining, capacity, retryAfterSeconds);
        }

    }
}
