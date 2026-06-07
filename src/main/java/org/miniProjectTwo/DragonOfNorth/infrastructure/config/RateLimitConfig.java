package org.miniProjectTwo.DragonOfNorth.infrastructure.config;

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
import org.miniProjectTwo.DragonOfNorth.shared.enums.RateLimitType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Infrastructure wiring for distributed rate limiting.
 * <p>
 * Creates Redis connectivity, Bucket4j proxy manager, and per-limit-type metrics counters. These
 * beans are consumed by rate-limit filters/services in security request processing. Changes to
 * codecs, Redis topology, or metric naming can affect runtime enforcement and observability.
 * </p>
 * <p>
 * This configuration ensures that rate limiting is properly implemented and monitored, providing
 * a consistent and reliable mechanism for managing API usage and preventing abuse. The use of
 * Redis for state storage, and Bucket4j for rate limiting provides a scalable and performant solution.
 * </p>
 */
@Configuration
public class RateLimitConfig {


    /**
     * Creates the distributed Bucket4j proxy manager.
     * The proxy manager is configured to use the provided Redis connection for storing bucket state, allowing for distributed rate limiting across multiple instances of the application. By using a proxy manager, we can ensure that all instances of the application share the same rate-limiting state, preventing issues such as one instance allowing more requests than others due to the local state. The proxy manager abstracts away the details of interacting with Redis, providing a simple interface for managing buckets and enforcing rate limits in the application code.
     * @param connection Redis connection used to store bucket state
     * @return distributed proxy manager
     */
    @Bean
    public ProxyManager<String> bucket4jProxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection).build();
    }

    /**
     * Creates per-type counters for blocked requests.
     * A counter is created for each rate-limit type defined in the {@link RateLimitType} enum, allowing for granular monitoring of rate-limiting events. Each counter is tagged with the corresponding rate-limit type (e.g., "ip", "user", "global") to facilitate filtering and aggregation in monitoring tools. The counters are registered with the provided Micrometer registry, ensuring that they are properly integrated into the application's metrics system and can be visualized in dashboards or used for alerting based on rate-limiting activity.
     * @param registry Micrometer registry
     * @return counters keyed by rate-limit type
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
     * Creates per-type counters for requests that pass rate limiting.
     * Similar to the blocked counters, this method creates a counter for each rate-limit type to track successful requests that are allowed through the rate limiter. Each counter is tagged with the rate-limit type for easy identification and analysis in monitoring tools.
     * @param registry Micrometer registry
     * @return counters keyed by rate-limit type
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
     * Creates the Redis client used by rate-limiting components.
     * The connection details are sourced from application properties, allowing for flexible configuration across environments. The client is configured to use the appropriate host, port, and optional password for secure access to the Redis instance. Proper resource management is ensured by specifying a destroy method to shut down the client when the application context is closed.
     * @param host Redis host
     * @param port Redis port
     * @param password optional Redis password
     * @return configured Redis client
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
     * Opens a Redis connection with codecs expected by Bucket4j.
     * The connection uses a string codec for keys (bucket identifiers) and a byte array codec for values (serialized bucket state). This configuration ensures compatibility with Bucket4j's storage format and allows for efficient storage and retrieval of the bucket state in Redis. The connection is also configured to be properly closed when the application context is shut down, preventing resource leaks.
     * @param redisClient Redis client bean
     * @return connection that stores keys as strings and values as byte arrays
     */
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

}
