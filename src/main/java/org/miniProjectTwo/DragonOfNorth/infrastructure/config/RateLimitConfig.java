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
 * Wires Redis-backed Bucket4j rate limiting and related metrics.
 */
@Configuration
public class RateLimitConfig {


    /**
     * Creates the distributed Bucket4j proxy manager.
     *
     * @param connection Redis connection used to store bucket state
     * @return distributed proxy manager
     */
    @Bean
    public ProxyManager<String> bucket4jProxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection).build();
    }

    /**
     * Creates per-type counters for blocked requests.
     *
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
     *
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
     * Creates the Redis client used by rate limiting components.
     *
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
     *
     * @param redisClient Redis client bean
     * @return connection that stores keys as strings and values as byte arrays
     */
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

}
