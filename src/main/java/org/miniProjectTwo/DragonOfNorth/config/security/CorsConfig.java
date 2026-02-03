package org.miniProjectTwo.DragonOfNorth.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Handles Cross-Origin Resource Sharing (CORS) configuration for the application.
 *
 * <p>This configuration enables controlled access from frontend clients by specifying
 * allowed origins, headers, methods, and exposed headers. It also logs the CORS setup
 * for debugging and verification during startup.
 *
 * <p>Designed for JWT-based APIs that use custom headers such as {@code Authorization}.
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    /**
     * Creates the {@link CorsConfigurationSource} used by Spring Security to apply
     * CORS rules to incoming HTTP requests.
     *
     * <p>Key behaviors:
     * <ul>
     *     <li>Allows local development origins (localhost & 127.0.0.1 with any port)</li>
     *     <li>Allows all HTTP methods and headers</li>
     *     <li>Exposes {@code Authorization} header for JWT-based authentication</li>
     *     <li>Enables credentials for cookies / tokens (if required)</li>
     * </ul>
     *
     * @return fully configured {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // Allowed origin patterns (supports wildcard ports)
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://dragon-of-north-bsj1km8hv-vinay2080s-projects.vercel.app"
        ));

        // Allow all headers and methods
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // Enable cookies / Authorization header
        config.setAllowCredentials(true);

        // Header that frontend must receive for JWT auth
        config.setExposedHeaders(List.of("Authorization"));

        // Logging for startup clarity
        log.info("CORS configuration initialized with allowed patterns: {}", config.getAllowedOriginPatterns());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
