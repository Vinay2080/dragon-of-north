package org.miniProjectTwo.DragonOfNorth.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.enums.RateLimitType;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RateLimitConfigTest {

    private final RateLimitConfig rateLimitConfig = new RateLimitConfig();

    @Test
    void rateLimitCounters_shouldCreateCounterForEachType() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        Map<RateLimitType, Counter> blocked = rateLimitConfig.rateLimitBlockedCounters(registry);
        Map<RateLimitType, Counter> success = rateLimitConfig.rateLimitSuccessCounters(registry);

        assertEquals(RateLimitType.values().length, blocked.size());
        assertEquals(RateLimitType.values().length, success.size());
    }

    @Test
    void redisClient_shouldCreateInstanceWithoutPassword() {
        RedisClient client = rateLimitConfig.redisClient("localhost", 6379, "");
        assertNotNull(client);
        client.shutdown();
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisConnection_shouldDelegateToRedisClient() {
        RedisClient redisClient = Mockito.mock(RedisClient.class);
        StatefulRedisConnection<String, byte[]> connection = Mockito.mock(StatefulRedisConnection.class);
        when(redisClient.connect(Mockito.any())).thenReturn(connection);

        StatefulRedisConnection<String, byte[]> actual = rateLimitConfig.redisConnection(redisClient);

        assertSame(connection, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    void bucket4jProxyManager_shouldCreateManager() {
        StatefulRedisConnection<String, byte[]> connection = Mockito.mock(StatefulRedisConnection.class);
        RedisCommands<String, byte[]> redisCommands = Mockito.mock(RedisCommands.class);
        when(connection.sync()).thenReturn(redisCommands);
        ProxyManager<String> manager = rateLimitConfig.bucket4jProxyManager(connection);
        assertNotNull(manager);
    }
}
