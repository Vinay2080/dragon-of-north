package org.miniProjectTwo.DragonOfNorth.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for the application.
 *
 * <p>This configuration:
 * <ul>
 *   <li>Enables web and method-level security</li>
 *   <li>Builds a stateless {@link SecurityFilterChain} for token-based (JWT) authentication</li>
 *   <li>Disables CSRF protection as the application is stateless</li>
 *   <li>Whitelists public endpoints defined in {@link #public_urls}</li>
 *   <li>Requires authentication for all other endpoints</li>
 *   <li>Registers a JWT filter for token validation</li>
 * </ul>
 *
 * @see JwtFilter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Ant-style request matcher patterns that are publicly accessible (no authentication required).
     * <p>Examples (to be filled as needed):
     * - "/auth/**" for authentication endpoints (login/register)
     * - "/swagger-ui/**" and "/v3/api-docs/**" for API documentation
     * - "/health", "/actuator/**" for health checks/actuator
     */
    // todo add urls later;
    public static final String[] public_urls = {
            "/api/v1/auth/**",
            "/api/v1/otp/**",

            //swagger ui and OpenAPI documentation
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger/resources/**",

            // actuator and health points
            "/actuator",
            "/actuator/health",
            "/actuator/health/**"
    };

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtFilter jwtFilter;

    /**
     * Configures the HTTP security filter chain.
     *
     * <p>Pipeline summary:
     * - Disables CSRF protections (suitable for stateless REST APIs).
     * - Allows anonymous access to {@link #public_urls}.
     * - Requires authentication for all other requests.
     * - Adds the {@link #jwtFilter} before {@link UsernamePasswordAuthenticationFilter}
     * so JWT tokens are processed prior to username/password auth.
     * - Sets session management to {@link SessionCreationPolicy#STATELESS}.
     *
     * @param httpSecurity the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     */

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity) {
        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests
                        (auth -> auth
                                .requestMatchers(public_urls).permitAll()
                                .anyRequest()
                                .authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

}
