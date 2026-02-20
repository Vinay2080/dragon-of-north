package org.miniProjectTwo.DragonOfNorth.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitPropertiesTest {

    @Test
    void nestedProperties_shouldStoreAndExposeValues() {
        RateLimitProperties.EndpointConfig endpoint = new RateLimitProperties.EndpointConfig();
        endpoint.setPattern("/api/v1/auth/**");
        endpoint.setType("LOGIN");

        RateLimitProperties.LimitRule rule = new RateLimitProperties.LimitRule();
        rule.setCapacity(10);
        rule.setRefillTokens(10);
        rule.setRefillMinutes(1);

        assertEquals("/api/v1/auth/**", endpoint.getPattern());
        assertEquals("LOGIN", endpoint.getType());
        assertEquals(10, rule.getCapacity());
        assertEquals(10, rule.getRefillTokens());
        assertEquals(1, rule.getRefillMinutes());
    }
}
