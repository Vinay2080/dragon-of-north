package org.miniProjectTwo.DragonOfNorth.infrastructure.config;

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
 * Binds external rate-limit settings.
 *
 * <p>Maps endpoint patterns to limit-rule definitions.</p>
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    /**
     * Endpoint definitions keyed by logical name.
     */
    private Map<String, EndpointConfig> endpoints = new HashMap<>();

    /**
     * Token-bucket rules keyed by rule name.
     */
    private Map<String, LimitRule> rules = new HashMap<>();

    /**
     * Endpoint configuration entry.
     */
    @Setter
    @Getter
    @Validated
    public static class EndpointConfig {

        /**
         * Ant-style URL pattern (for example: {@code /api/v1/auth/**}).
         */
        @NotBlank
        private String pattern;

        /**
         * Rule key to apply for matching requests.
         */
        @NotNull
        private String type;
    }

    /**
     * Token-bucket rule configuration.
     */
    @Setter
    @Getter
    public static class LimitRule {
        /**
         * Maximum tokens in the bucket (burst size).
         */
        private int capacity;

        /**
         * Tokens added per refill interval.
         */
        private int refillTokens;

        /**
         * Refill interval in minutes.
         */
        private int refillMinutes;
    }

}
