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

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private Map<String, EndpointConfig> endpoints = new HashMap<>();
    private Map<String, LimitRule> rules = new HashMap<>();

    @Setter
    @Getter
    @Validated
    public static class EndpointConfig {
        @NotBlank
        private String pattern;
        @NotNull
        private String type;

    }

    @Setter
    @Getter
    public static class LimitRule {
        private int capacity;
        private int refillTokens;
        private int refillMinutes;

    }

}
