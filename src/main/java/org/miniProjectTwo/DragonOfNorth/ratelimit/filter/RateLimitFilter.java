package org.miniProjectTwo.DragonOfNorth.ratelimit.filter;

import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.infrastructure.config.RateLimitProperties;
import org.miniProjectTwo.DragonOfNorth.ratelimit.resolver.RateLimitKeyResolver;
import org.miniProjectTwo.DragonOfNorth.ratelimit.service.RateLimitBucketService;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RateLimitType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditContext;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Request an interception layer that enforces per-policy rate limits before controller execution.
 * <p>
 * Depends on key resolution and distributed bucket state; positioned to mitigate brute-force and
 * abuse traffic before expensive authentication/service work is performed.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitBucketService bucketService;
    private final RateLimitKeyResolver keyResolver;
    private final RateLimitProperties properties;
    private final Map<RateLimitType, Counter> blockedCounters;
    private final Map<RateLimitType, Counter> successCounters;
    private final AuditEventLogger auditEventLogger;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(RateLimitBucketService bucketService,
                           RateLimitKeyResolver keyResolver,
                           RateLimitProperties properties,
                           Map<RateLimitType, Counter> rateLimitBlockedCounters,
                           Map<RateLimitType, Counter> rateLimitSuccessCounters,
                           AuditEventLogger auditEventLogger) {
        this.bucketService = bucketService;
        this.keyResolver = keyResolver;
        this.properties = properties;
        this.blockedCounters = rateLimitBlockedCounters;
        this.successCounters = rateLimitSuccessCounters;
        this.auditEventLogger = auditEventLogger;
    }

    /**
     * Processes the incoming request and applies rate limiting based on the configured properties and key resolver.
     *
     * @param request     The incoming HTTP request.
     * @param response    The HTTP response to be sent.
     * @param filterChain The filter chain for further processing.
     * @throws ServletException If there's an error during request processing.
     * @throws IOException      If there's an I/O error during request processing.
     */
    @Override
    public void doFilterInternal(HttpServletRequest request,
                                 @NonNull HttpServletResponse response,
                                 @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        RateLimitType type = matchEndpoint(path);

        if (type != null) {
            String key = keyResolver.resolve(request, type);
            RateLimitBucketService.ConsumptionResult result = bucketService.tryConsume(key, type);

            // Add response headers
            response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
            response.setHeader("X-RateLimit-Capacity", String.valueOf(result.getCapacity()));

            if (!result.isAllowed()) {
                response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
                blockedCounters.get(type).increment();

                auditEventLogger.logSecurity(SecurityAuditEvent.AUTH_ABUSE_RATE_LIMITED, new SecurityAuditContext(
                        null, null, request.getHeader("X-Device-Id"), request.getHeader("X-Request-Id"), request.getRemoteAddr(), null,
                        "rate_limit", type.name(), "failure", "retry_after=" + result.getRetryAfterSeconds(), null
                ));
                if (type == RateLimitType.STEP_UP_REQUEST || type == RateLimitType.MFA_VERIFY) {
                    auditEventLogger.log(SecurityAuditEvent.AUTH_ABUSE_CHALLENGE_FLOOD, null, request.getHeader("X-Device-Id"), request.getRemoteAddr(), "failure", type.name().toLowerCase(), request.getHeader("X-Request-Id"));
                }
                if (type == RateLimitType.MFA_VERIFY || type == RateLimitType.STEP_UP_VERIFY) {
                    auditEventLogger.log(SecurityAuditEvent.AUTH_ABUSE_MFA_BRUTEFORCE, null, request.getHeader("X-Device-Id"), request.getRemoteAddr(), "failure", type.name().toLowerCase(), request.getHeader("X-Request-Id"));
                }
                log.warn("Rate limit exceeded for type={}, retryAfter={}s", type, result.getRetryAfterSeconds());
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }

            successCounters.get(type).increment();
            log.debug("Rate limit check passed for key={}, type={}, remaining={}",
                    key, type, result.getRemaining());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Matches the request path against configured endpoint patterns to determine the rate limit type.
     *
     * @param requestPath The path of the incoming request.
     * @return The rate limit type associated with the matched endpoint, or null if no match is found.
     */
    private RateLimitType matchEndpoint(String requestPath) {
        for (Map.Entry<String, RateLimitProperties.EndpointConfig> entry : properties.getEndpoints().entrySet()) {
            RateLimitProperties.EndpointConfig config = entry.getValue();
            if (pathMatcher.match(config.getPattern(), requestPath)) {
                return RateLimitType.valueOf(config.getType().toUpperCase());
            }
        }
        return null;
    }
}
