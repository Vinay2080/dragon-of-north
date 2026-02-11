package org.miniProjectTwo.DragonOfNorth.ratelimit;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.config.RateLimitProperties;
import org.miniProjectTwo.DragonOfNorth.enums.RateLimitType;

import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitBucketServiceTest {

    private static RateLimitProperties.LimitRule rule() {
        RateLimitProperties.LimitRule rule = new RateLimitProperties.LimitRule();
        rule.setCapacity(10);
        rule.setRefillTokens(10);
        rule.setRefillMinutes(1);
        return rule;
    }

    @SuppressWarnings("unchecked")
    private static Supplier<BucketConfiguration> anySupplier() {
        return any(Supplier.class);
    }

    @Test
    void initializeConfigurations_shouldThrow_whenRuleMissing() {
        // arrange
        @SuppressWarnings("unchecked")
        ProxyManager<String> proxyManager = mock(ProxyManager.class);

        RateLimitProperties properties = new RateLimitProperties();
        properties.setRules(Map.of());

        RateLimitBucketService service = new RateLimitBucketService(proxyManager, properties);

        // act + assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, service::initializeConfigurations);
        assertTrue(ex.getMessage().contains("Missing rate limit configuration"));
    }

    @Test
    void initializeConfigurations_shouldSucceed_whenAllRulesPresent() {
        // arrange
        @SuppressWarnings("unchecked")
        ProxyManager<String> proxyManager = mock(ProxyManager.class);

        RateLimitProperties properties = new RateLimitProperties();
        properties.setRules(Map.of(
                "signup", rule(),
                "login", rule(),
                "otp", rule()
        ));

        RateLimitBucketService service = new RateLimitBucketService(proxyManager, properties);

        // act + assert
        assertDoesNotThrow(service::initializeConfigurations);
    }

    @Test
    void tryConsume_shouldReturnAllowed_whenProbeConsumed() {
        // arrange
        @SuppressWarnings("unchecked")
        ProxyManager<String> proxyManager = mock(ProxyManager.class, RETURNS_DEEP_STUBS);
        BucketProxy bucket = mock(BucketProxy.class);

        when(proxyManager.builder().build(eq("k"), anySupplier())).thenReturn(bucket);

        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(7L);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        RateLimitProperties properties = new RateLimitProperties();
        properties.setRules(Map.of(
                "signup", rule(),
                "login", rule(),
                "otp", rule()
        ));

        RateLimitBucketService service = new RateLimitBucketService(proxyManager, properties);
        service.initializeConfigurations();

        // act
        RateLimitBucketService.ConsumptionResult result = service.tryConsume("k", RateLimitType.OTP);

        // assert
        assertTrue(result.isAllowed());
        assertEquals(7L, result.getRemaining());
        assertEquals(10L, result.getCapacity());
        assertEquals(0L, result.getRetryAfterSeconds());
    }

    @Test
    void tryConsume_shouldReturnBlocked_whenProbeNotConsumed() {
        // arrange
        @SuppressWarnings("unchecked")
        ProxyManager<String> proxyManager = mock(ProxyManager.class, RETURNS_DEEP_STUBS);
        BucketProxy bucket = mock(BucketProxy.class);

        when(proxyManager.builder().build(eq("k"), anySupplier())).thenReturn(bucket);

        ConsumptionProbe probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(false);
        when(probe.getRemainingTokens()).thenReturn(0L);
        when(probe.getNanosToWaitForRefill()).thenReturn(3_000_000_000L); // 3s
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);

        RateLimitProperties properties = new RateLimitProperties();
        properties.setRules(Map.of(
                "signup", rule(),
                "login", rule(),
                "otp", rule()
        ));

        RateLimitBucketService service = new RateLimitBucketService(proxyManager, properties);
        service.initializeConfigurations();

        // act
        RateLimitBucketService.ConsumptionResult result = service.tryConsume("k", RateLimitType.OTP);

        // assert
        assertFalse(result.isAllowed());
        assertEquals(0L, result.getRemaining());
        assertEquals(10L, result.getCapacity());
        assertEquals(4L, result.getRetryAfterSeconds());
    }

    @Test
    void tryConsume_shouldFailOpen_whenProxyThrows() {
        // arrange
        @SuppressWarnings("unchecked")
        ProxyManager<String> proxyManager = mock(ProxyManager.class);
        when(proxyManager.builder()).thenThrow(new RuntimeException("redis down"));

        RateLimitProperties properties = new RateLimitProperties();
        properties.setRules(Map.of(
                "signup", rule(),
                "login", rule(),
                "otp", rule()
        ));

        RateLimitBucketService service = new RateLimitBucketService(proxyManager, properties);
        service.initializeConfigurations();

        // act
        RateLimitBucketService.ConsumptionResult result = service.tryConsume("k", RateLimitType.OTP);

        // assert
        assertTrue(result.isAllowed());
        assertEquals(0L, result.getRemaining());
        assertEquals(0L, result.getCapacity());
    }
}
