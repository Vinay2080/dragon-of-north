package org.miniProjectTwo.DragonOfNorth.security.config;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.security.filter.CsrfCookieFilter;
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
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for the application.
 *
 * <p>This configuration:
 * <ul>
 *   <li>Enables web and method-level security</li>
 *   <li>Builds a stateless {@link SecurityFilterChain} for token-based (JWT) authentication</li>
 *   <li>Uses cookie-based CSRF protection via {@link CookieCsrfTokenRepository}</li>
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

    public static final String[] public_urls = {
            "/api/v1/auth/csrf",
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
     * Keep bypass scoped to pre-auth auth endpoints only.
     */
    public static final String[] csrf_bypass_urls = {
            "/api/v1/auth/identifier/**",
            "/api/v1/auth/jwt/refresh",
            "/api/v1/auth/oauth/**",
            "/api/v1/auth/password/forgot/**",
            "/api/v1/otp/**"
    };

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtFilter jwtFilter;
    private final CsrfCookieFilter csrfCookieFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

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
                .addFilterAfter(csrfCookieFilter, CsrfFilter.class)
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
