package org.miniProjectTwo.DragonOfNorth.config.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    private final CorsConfig corsConfig = new CorsConfig();

    @Test
    void corsConfigurationSource_shouldRegisterExpectedRules() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        assertNotNull(source);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/identifier/status");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);
        assertTrue(config.getAllowedOriginPatterns().contains("http://localhost:*"));
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowCredentials());
        assertTrue(config.getExposedHeaders().contains("Authorization"));
    }
}
