package org.miniProjectTwo.DragonOfNorth.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configures Cross-Origin Resource Sharing (CORS) for HTTP endpoints.
 * This configuration ensures that only trusted origins are allowed to access the API,
 * and that only necessary HTTP methods and headers are permitted. It also includes
 * support for wildcard ports in local development environments.
 *
 * <p>Security considerations: By limiting origins and methods, we mitigate the risk of
 * cross-site request forgery (CSRF) and unauthorized access. The use of wildcard ports
 * in local development is a convenience feature that should be disabled in production.
 * </p>
 */
@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    /**
     * Builds the CORS policy used by Spring Security.
     * <ol>
     *   <li>Allowed origin patterns include localhost with any port, and specific production domains.</li>
     *   <li>Allowed methods cover common HTTP verbs used by the API.</li>
     *   <li>Allowed headers include those necessary for content negotiation, authentication, and AJAX requests.</li>
     *   <li>Credentials are allowed to support cookie-based authentication.</li>
     *   <li>Exposed headers include "Authorization" for JWT token handling on the frontend.</li>
     * </ol>
     * @return configuration source applied to all request paths
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // Allowed origin patterns (supports wildcard ports)
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://app.verloren.dev",
                "https://api.verloren.dev"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "X-XSRF-TOKEN",
                "X-Requested-With"));

        // Enable cookies / Authorization header
        config.setAllowCredentials(true);

        // Header that frontend must receive for JWT auth
        config.setExposedHeaders(List.of("Authorization"));

        log.debug("CORS allowed origin patterns: {}", config.getAllowedOriginPatterns());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
