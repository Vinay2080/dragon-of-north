package org.miniProjectTwo.DragonOfNorth.ratelimit;

import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.config.RateLimitProperties;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.enums.RateLimitType;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.resolver.RateLimitKeyResolver;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.RateLimitBucketService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitBucketService bucketService;
    private final RateLimitKeyResolver keyResolver;
    private final RateLimitProperties properties;
    private final Map<RateLimitType, Counter> blockedCounters;
    private final Map<RateLimitType, Counter> successCounters;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(RateLimitBucketService bucketService,
                           RateLimitKeyResolver keyResolver,
                           RateLimitProperties properties,
                           Map<RateLimitType, Counter> rateLimitBlockedCounters,
                           Map<RateLimitType, Counter> rateLimitSuccessCounters) {
        this.bucketService = bucketService;
        this.keyResolver = keyResolver;
        this.properties = properties;
        this.blockedCounters = rateLimitBlockedCounters;
        this.successCounters = rateLimitSuccessCounters;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        RateLimitType type = matchEndpoint(path);

        if (type != null) {
            String key = keyResolver.resolve(request, type);
            RateLimitBucketServiceImpl.ConsumptionResult result = bucketService.tryConsume(key, type);

            // Add response headers
            response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
            response.setHeader("X-RateLimit-Capacity", String.valueOf(result.getCapacity()));

            if (!result.isAllowed()) {
                response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));
                blockedCounters.get(type).increment();

                log.warn("Rate limit exceeded for key={}, type={}, retryAfter={}s",
                        key, type, result.getRetryAfterSeconds());
                throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }

            successCounters.get(type).increment();
            log.debug("Rate limit check passed for key={}, type={}, remaining={}",
                    key, type, result.getRemaining());
        }

        filterChain.doFilter(request, response);
    }

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
