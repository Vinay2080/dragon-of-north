package org.miniProjectTwo.DragonOfNorth.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.enums.RateLimitType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitKeyResolverTest {

    private final RateLimitKeyResolver resolver = new RateLimitKeyResolver();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolve_shouldUseUserName_whenAuthenticated() {
        // arrange
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, Collections.emptyList())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5");

        // act
        String key = resolver.resolve(request, RateLimitType.OTP);

        // assert
        assertEquals("OTP:user:alice", key);
    }

    @Test
    void resolve_shouldUseIp_whenAnonymous() {
        // arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.7");

        // act
        String key = resolver.resolve(request, RateLimitType.LOGIN);

        // assert
        assertEquals("LOGIN:ip:10.0.0.7", key);
    }

    @Test
    void resolve_shouldUseXForwardedForFirstIp_whenPresent() {
        // arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 70.41.3.18");
        request.setRemoteAddr("10.0.0.9");

        // act
        String key = resolver.resolve(request, RateLimitType.SIGNUP);

        // assert
        assertEquals("SIGNUP:ip:203.0.113.10", key);
    }
}
