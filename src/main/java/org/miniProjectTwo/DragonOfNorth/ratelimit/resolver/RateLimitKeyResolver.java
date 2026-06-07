package org.miniProjectTwo.DragonOfNorth.ratelimit.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RateLimitType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * RateLimitKeyResolver is responsible for generating unique keys for rate limiting based on the request context and rate limit type.
 * This resolver extracts relevant information from the HTTP request and security context to create a key that can be used
 * to identify and track rate limit usage for different types of operations.
 */
@Component
public class RateLimitKeyResolver {

    /**
     * Generates a unique key for rate limiting based on the request context and rate limit type.
     *
     * @param request The HTTP request containing client information.
     * @param type    The type of rate limit for which to generate the key.
     * @return A string key that uniquely identifies the rate limit usage for the given request and type.
     */
    public String resolve(HttpServletRequest request, RateLimitType type) {
        String ip = getClientIp(request);
        String deviceId = normalize(request.getHeader("X-Device-Id"));
        String challengeId = normalize(request.getHeader("X-Challenge-Id"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityPrincipal principal = authentication != null && authentication.getPrincipal() instanceof SecurityPrincipal p ? p : null;

        return switch (type) {
            case LOGIN, SIGNUP, OTP, PASSWORDLESS -> type.name() + ":ip:" + ip + ":device:" + n(deviceId);
            case MFA_VERIFY -> type.name() + ":ip:" + ip + ":challenge:" + n(challengeId) + ":device:" + n(deviceId);
            case STEP_UP_REQUEST, STEP_UP_VERIFY -> type.name() + ":user:" + (principal == null ? "anon" : principal.userId()) + ":sid:" + (principal == null ? "none" : principal.sessionId()) + ":ip:" + ip;
            case REFRESH -> type.name() + ":ip:" + ip + ":device:" + n(deviceId) + ":user:" + (principal == null ? "anon" : principal.userId());
        };
    }

    /**
     * Normalizes a string value by trimming whitespace and returning null if the result is blank.
     *
     * @return The normalized string or null if the input is null or blank after trimming.
     */
    private String n(String v) { return v == null ? "none" : v; }
    private String normalize(String value) {
        if (value == null) return null;
        String t=value.trim();
        return t.isBlank()?null:t;
    }
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        else ip = ip.split(",")[0].trim();
        return ip;
    }
}
