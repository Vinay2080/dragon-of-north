package org.miniProjectTwo.DragonOfNorth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for rate limiting settings.
 * Binds to {@code rate-limit} prefix in application properties to configure
 * rate limiting for different endpoints and their corresponding limit rules.
 * Supports flexible configuration of endpoint patterns and token bucket parameters.
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /**
     * Map of endpoint configurations keyed by the endpoint name.
     * Each entry defines the URL pattern and rate limit type for specific endpoints.
     */
    private Map<String, EndpointConfig> endpoints = new HashMap<>();

    /**
     * Map of limit rules keyed by rule name.
     * Each entry defines token bucket parameters (capacity, refill rate) for rate limiting.
     */
    private Map<String, LimitRule> rules = new HashMap<>();

    /**
     * Configuration for a specific rate-limited endpoint.
     * Defines the URL pattern to match and the type of rate limiting
     * rule that should be applied to matching requests.
     */
    @Setter
    @Getter
    @Validated
    public static class EndpointConfig {

        /**
         * The URL pattern for matching requests (e.g., "/api/v1/auth/**").
         * Uses Ant-style path patterns for flexible endpoint matching.
         */
        @NotBlank
        private String pattern;

        /**
         * The rate limit type that references a rule in the rules map.
         * Determines which token bucket configuration to apply.
         */
        @NotNull
        private String type;
    }

    /**
     * Token bucket configuration for rate limiting rules.
     * Defines the bucket capacity and refill rate using the token bucket algorithm.
     * Controls how many requests are allowed within a time window.
     */
    @Setter
    @Getter
    public static class LimitRule {
        /**
         * The maximum number of tokens the bucket can hold.
         * Represents the burst capacity - maximum requests allowed in a short time.
         */
        private int capacity;

        /**
         * The number of tokens to add to the bucket during each refill.
         * Controls the steady-state request rate after the burst is consumed.
         */
        private int refillTokens;

        /**
         * The time interval in minutes between token refills.
         * Works with refillTokens to determine the sustained request rate.
         */
        private int refillMinutes;
    }

}
