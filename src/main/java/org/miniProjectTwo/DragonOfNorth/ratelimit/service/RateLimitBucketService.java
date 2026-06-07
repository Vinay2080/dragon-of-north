package org.miniProjectTwo.DragonOfNorth.ratelimit.service;

import io.github.bucket4j.BucketConfiguration;
import lombok.Getter;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RateLimitType;

/**
 * Service for managing rate limit buckets.
 */
public interface RateLimitBucketService {
    /**
     * Initializes rate limit bucket configurations.
     */
    void initializeConfigurations();

    /**
     * Attempts to consume tokens from a rate limit bucket.
     *
     * @param key  Unique identifier for the rate limit bucket
     * @param type Type of rate limit
     * @return ConsumptionResult indicating whether the operation was allowed and details
     */
    ConsumptionResult tryConsume(String key, RateLimitType type);

    /**
     * Creates a bucket configuration for the specified rate limit type.
     *
     * @param type Type of rate limit
     * @return BucketConfiguration for the specified rate limit type
     */
    BucketConfiguration createBucketConfiguration(RateLimitType type);

    @Getter
    class ConsumptionResult {
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
