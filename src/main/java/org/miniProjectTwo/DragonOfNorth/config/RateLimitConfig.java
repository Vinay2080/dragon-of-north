package org.miniProjectTwo.DragonOfNorth.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.miniProjectTwo.DragonOfNorth.enums.RateLimitType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Configuration class for rate limiting using Bucket4j with Redis backend.
 * Configures distributed rate limiting with Redis for storing bucket state,
 * metrics for monitoring rate limit behavior, and Redis client connections.
 * This setup enables rate limiting across multiple application instances
 * for authentication endpoints and other sensitive operations.
 */
@Configuration
public class RateLimitConfig {


    /**
     * Creates a distributed proxy manager for Bucket4j using Redis.
     * Provides distributed bucket storage that works across multiple application
     * instances, ensuring rate limits are enforced consistently in a clustered environment.
     *
     * @param connection the Redis connection for distributed storage
     * @return a ProxyManager for distributed bucket operations
     */
    @Bean
    public ProxyManager<String> bucket4jProxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection).build();
    }

    /**
     * Creates counters for monitoring blocked requests due to rate limiting.
     * Provides metrics for each rate limit type to track how many requests
     * are being blocked, useful for monitoring and tuning rate limit thresholds.
     *
     * @param registry the Micrometer meter registry for metrics registration
     * @return a map of counters indexed by rate limit type
     */
    @Bean
    public Map<RateLimitType, Counter> rateLimitBlockedCounters(MeterRegistry registry) {
        Map<RateLimitType, Counter> counters = new EnumMap<>(RateLimitType.class);
        for (RateLimitType type : RateLimitType.values()) {
            counters.put(type, Counter.builder("rate_limit.blocked")
                    .tag("type", type.name().toLowerCase())
                    .description("Number of blocked requests due to rate limiting")
                    .register(registry));
        }
        return counters;
    }

    /**
     * Creates counters for monitoring successful requests that pass rate limiting.
     * Provides metrics for each rate limit type to track successful request rates,
     * useful for understanding traffic patterns and rate limit effectiveness.
     *
     * @param registry the Micrometer meter registry for metrics registration
     * @return a map of counters indexed by rate limit type
     */
    @Bean
    public Map<RateLimitType, Counter> rateLimitSuccessCounters(MeterRegistry registry) {
        Map<RateLimitType, Counter> counters = new EnumMap<>(RateLimitType.class);
        for (RateLimitType type : RateLimitType.values()) {
            counters.put(type, Counter.builder("rate_limit.success")
                    .tag("type", type.name().toLowerCase())
                    .description("Number of successful requests")
                    .register(registry));
        }
        return counters;
    }

    /**
     * Creates and configures the Redis client for rate limiting storage.
     * Establishes connection to Redis server using configured host, port,
     * and optional password. The client is configured for proper shutdown
     * when the application context closes.
     *
     * @param host     Redis server host from application properties
     * @param port     Redis server port (defaults to 6379)
     * @param password optional Redis password (can be empty)
     * @return a configured RedisClient instance
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(@Value("${spring.data.redis.host}") String host,
                                   @Value("${spring.data.redis.port:6379}") int port,
                                   @Value("${spring.data.redis.password:}") String password) {
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(host)
                .withPort(port);

        if (password != null && !password.isEmpty()) {
            uriBuilder.withPassword(password.toCharArray());
        }

        return RedisClient.create(uriBuilder.build());
    }

    /**
     * Creates a Redis connection with the appropriate codec for rate limiting.
     * Establishes a connection using String codec for keys and ByteArray codec
     * for values, which is optimal for storing Bucket4j bucket state in Redis.
     * The connection is properly closed when the application context shuts down.
     *
     * @param redisClient the configured Redis client
     * @return a StatefulRedisConnection with appropriate codecs for rate limiting
     */
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

}
