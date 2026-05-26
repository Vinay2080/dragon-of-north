package org.miniProjectTwo.DragonOfNorth.ratelimit.resolver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RateLimitType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitKeyResolverTest {

    private final RateLimitKeyResolver resolver = new RateLimitKeyResolver();

    @BeforeEach
    void setUp() { SecurityContextHolder.clearContext(); }
    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void resolve_shouldUseIpAndDevice_forPublicLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.7");
        request.addHeader("X-Device-Id", "device-1");

        String key = resolver.resolve(request, RateLimitType.LOGIN);
        assertTrue(key.contains("LOGIN:ip:10.0.0.7:device:device-1"));
    }

    @Test
    void resolve_shouldUseChallengeDimension_forMfaVerify() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.8");
        request.addHeader("X-Challenge-Id", "challenge-1");

        String key = resolver.resolve(request, RateLimitType.MFA_VERIFY);
        assertTrue(key.contains("MFA_VERIFY:ip:203.0.113.8:challenge:challenge-1"));
    }

    @Test
    void resolve_shouldUseUserSid_forStepUpVerify() {
        SecurityPrincipal principal = new SecurityPrincipal(UUID.randomUUID(), List.of(), true, null, UUID.randomUUID(), List.of("pwd"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.9");

        String key = resolver.resolve(request, RateLimitType.STEP_UP_VERIFY);
        assertTrue(key.contains("STEP_UP_VERIFY:user:"));
        assertTrue(key.contains(":sid:"));
    }
}
