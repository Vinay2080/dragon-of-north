package org.miniProjectTwo.DragonOfNorth.ratelimit.service.impl;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.infrastructure.config.RateLimitProperties;
import org.miniProjectTwo.DragonOfNorth.ratelimit.service.RateLimitBucketService;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RateLimitType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Bucket4j-backed distributed rate-limit service using Redis proxy manager state.
 * <p>
 * Concurrency behavior depends on backend CAS semantics; policy changes affect both security and UX.
 */
@Service
@Slf4j
public class RateLimitBucketServiceImpl implements RateLimitBucketService {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties properties;
    private final Map<RateLimitType, BucketConfiguration> configCache = new EnumMap<>(RateLimitType.class);

    public RateLimitBucketServiceImpl(ProxyManager<String> proxyManager, RateLimitProperties properties) {
        this.proxyManager = proxyManager;
        this.properties = properties;
    }

    /**
     * Initializes rate limit configurations based on application properties.
     * This method iterates over all defined rate limit types and retrieves their configuration
     * rules from the application properties. If a rule is missing for a type, it throws an
     * exception, prompting the user to add the missing configuration to the application.yml file.
     * The method then creates and caches a BucketConfiguration for each rate limit type, logging
     * the initialization details for each type.
     *
     */
    @PostConstruct
    @Override
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

    /**
     * Attempts to consume a token from the rate limit bucket for the given key and type.
     * This method uses a proxy manager to retrieve the bucket configuration for the specified
     * rate limit type. It then tries to consume a token from the bucket and returns a ConsumptionResult
     * indicating whether the operation was allowed or blocked, along with relevant details.
     */
    @Override
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
            return ConsumptionResult.blocked(0, 0, 60);
        }


    }

    /**
     * Creates a BucketConfiguration for the specified rate limit type based on the application properties.
     * This method retrieves the rate limit rule for the given type from the application properties and
     * configures a bucket with the specified capacity and refill rate. The configuration is then returned
     * for use in rate-limiting operations.
     *
     */
    @Override
    public BucketConfiguration createBucketConfiguration(RateLimitType type) {
        RateLimitProperties.LimitRule rule = properties.getRules().get(type.name().toLowerCase());

        return BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(rule.getCapacity())
                        .refillGreedy(rule.getRefillTokens(), Duration.ofMinutes(rule.getRefillMinutes())))
                .build();
    }

}
