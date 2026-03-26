package org.miniProjectTwo.DragonOfNorth.security.config;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.security.filter.JwtFilter;
import org.miniProjectTwo.DragonOfNorth.security.handler.RestAccessDeniedHandler;
import org.miniProjectTwo.DragonOfNorth.security.handler.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
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
    public static final String[] public_urls = {
            "/api/v1/auth/identifier/status",
            "/api/v1/auth/identifier/sign-up",
            "/api/v1/auth/identifier/sign-up/complete",
            "/api/v1/auth/identifier/login",
            "/api/v1/auth/identifier/logout",
            "/api/v1/auth/jwt/refresh",
            "/api/v1/auth/oauth/google",
            "/api/v1/auth/oauth/google/signup",
            "/api/v1/auth/password/forgot/request",
            "/api/v1/auth/password/forgot/reset",
            "/api/v1/otp/**",

            //swagger ui and OpenAPI documentation
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger/resources/**",

            // actuator and health points
            "/actuator",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/prometheus"
    };

    /**
     * Ant-style request matcher patterns that bypass CSRF protection.
     * <p>These endpoints are publicly accessible or have specific security requirements
     * that make CSRF protection unnecessary or problematic:
     * - Actuator and documentation endpoints (public access)
     * - Pre-authentication endpoints (no CSRF tokens available yet)
     * - OAuth endpoints (external token exchange flows)
     */
    public static final String[] csrf_bypass_urls = {
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            // Public pre-auth lookup endpoint; keep it callable without CSRF bootstrap race.
            "/api/v1/auth/identifier/status",
            // Public login endpoint should not fail when the browser token bootstrap is out of sync.
            "/api/v1/auth/identifier/login",
            // Google OAuth token exchange is a public pre-auth flow.
            "/api/v1/auth/oauth/google",
            "/api/v1/auth/oauth/google/signup"
    };

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtFilter jwtFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    /**
     * Configures the HTTP security filter chain.
     *
     * <p>Pipeline summary:
     * - Enables cookie-based CSRF protection for browser requests.
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
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(csrf_bypass_urls))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests
                        (auth -> auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers(public_urls).permitAll()
                                .anyRequest()
                                .authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'"))
                        .xssProtection(
                                HeadersConfigurer.XXssConfig::disable
                        )
                        .httpStrictTransportSecurity(
                                hstsConfig -> hstsConfig.includeSubDomains(true)
                                        .maxAgeInSeconds(31536000)
                        )
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .build();
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie
                .sameSite(cookieSameSite)
                .secure(cookieSecure));
        return repository;
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
