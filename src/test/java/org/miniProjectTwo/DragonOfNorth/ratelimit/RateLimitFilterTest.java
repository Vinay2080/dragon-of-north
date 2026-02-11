package org.miniProjectTwo.DragonOfNorth.ratelimit;

import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.config.RateLimitProperties;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.enums.RateLimitType;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitBucketService bucketService;
    private RateLimitKeyResolver keyResolver;
    private FilterChain chain;

    private Map<RateLimitType, Counter> blockedCounters;
    private Map<RateLimitType, Counter> successCounters;

    private static RateLimitProperties propertiesFor() {
        RateLimitProperties props = new RateLimitProperties();
        RateLimitProperties.EndpointConfig cfg = new RateLimitProperties.EndpointConfig();
        cfg.setPattern("/api/v1/otp/**");
        cfg.setType(RateLimitType.OTP.name().toLowerCase());
        props.setEndpoints(Map.of("k", cfg));
        return props;
    }

    @BeforeEach
    void setUp() {
        bucketService = mock(RateLimitBucketService.class);
        keyResolver = mock(RateLimitKeyResolver.class);
        chain = mock(FilterChain.class);

        blockedCounters = new EnumMap<>(RateLimitType.class);
        successCounters = new EnumMap<>(RateLimitType.class);

        for (RateLimitType t : RateLimitType.values()) {
            blockedCounters.put(t, mock(Counter.class));
            successCounters.put(t, mock(Counter.class));
        }
    }

    @Test
    void doFilterInternal_shouldDelegate_whenNoEndpointMatched() throws Exception {
        // arrange
        RateLimitProperties props = new RateLimitProperties();
        // no endpoints configured => matchEndpoint returns null

        RateLimitFilter filter = new RateLimitFilter(bucketService, keyResolver, props, blockedCounters, successCounters);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/unknown");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // act
        filter.doFilterInternal(request, response, chain);

        // assert
        verify(chain).doFilter(request, response);
        verifyNoInteractions(bucketService);
        verifyNoInteractions(keyResolver);
        assertNull(response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    void doFilterInternal_shouldAddHeadersAndDelegate_whenAllowed() throws Exception {
        // arrange
        RateLimitProperties props = propertiesFor();

        RateLimitFilter filter = new RateLimitFilter(bucketService, keyResolver, props, blockedCounters, successCounters);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/otp/email/request");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(keyResolver.resolve(request, RateLimitType.OTP)).thenReturn("OTP:ip:1.2.3.4");
        when(bucketService.tryConsume("OTP:ip:1.2.3.4", RateLimitType.OTP))
                .thenReturn(RateLimitBucketService.ConsumptionResult.allowed(9, 10));

        // act
        filter.doFilterInternal(request, response, chain);

        // assert
        assertEquals("9", response.getHeader("X-RateLimit-Remaining"));
        assertEquals("10", response.getHeader("X-RateLimit-Capacity"));
        assertNull(response.getHeader("Retry-After"));

        verify(successCounters.get(RateLimitType.OTP)).increment();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldThrowBusinessException_whenBlocked() throws Exception {
        // arrange
        RateLimitProperties props = propertiesFor();

        RateLimitFilter filter = new RateLimitFilter(bucketService, keyResolver, props, blockedCounters, successCounters);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/otp/email/request");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(keyResolver.resolve(request, RateLimitType.OTP)).thenReturn("OTP:ip:1.2.3.4");
        when(bucketService.tryConsume("OTP:ip:1.2.3.4", RateLimitType.OTP))
                .thenReturn(RateLimitBucketService.ConsumptionResult.blocked(0, 10, 60));

        // act
        BusinessException ex = assertThrows(BusinessException.class,
                () -> filter.doFilterInternal(request, response, chain));

        // assert
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, ex.getErrorCode());
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        assertEquals("10", response.getHeader("X-RateLimit-Capacity"));
        assertEquals("60", response.getHeader("Retry-After"));
        verify(blockedCounters.get(RateLimitType.OTP)).increment();
        verify(chain, never()).doFilter(request, response);
    }
}
