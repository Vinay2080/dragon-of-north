package org.miniProjectTwo.DragonOfNorth.config.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * JWT authentication filter responsible for processing and validating JWT tokens
 * sent by the client in the {@code Authorization} header.
 *
 * <p>This filter runs once per request and performs the following step:
 * <ol>
 *   <li>Skips configured public authentication endpoints</li>
 *   <li>Extracts and parses the JWT from the {@code Bearer} header</li>
 *   <li>Validates token integrity, expiration, and ownership</li>
 *   <li>Loads the corresponding user</li>
 *   <li>Populates the Spring Security context with an authenticated user</li>
 * </ol>
 *
 * <p>This filter only sets authentication if:
 * <ul>
 *   <li>A valid token is provided</li>
 *   <li>The security context is currently unauthenticated</li>
 * </ul>
 *
 * <p>Any invalid or expired token results only in a silent skip — no. 500 errors —
 * allowing downstream exception handlers or access rules to handle unauthorized access.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtServices jwtServices;

    private final static String ROLES = "roles";


    private static final List<String> SWAGGER_PATH = List.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-ui.html"
    );


    private static final List<String> PUBLIC_PATH = List.of(
            "/api/v1/auth",
            "/api/v1/otp"
    );

    private boolean isPublic(String path) {
        return PUBLIC_PATH.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String path = request.getServletPath();

        // Public endpoints do not require JWT
        if (isPublic(path) || isSwagger(path)) {
            log.debug("Skipping JWT filter for public path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            log.debug("No JWT token found in request for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try {

            Claims claims = jwtServices.extractAllClaims(token);

            String tokenType = claims.get("token_type", String.class);
            if (!"access_token".equals(tokenType)) {
                filterChain.doFilter(request, response);
                return;
            }


            String subject = claims.getSubject();


            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId = UUID.fromString(subject);

                List<GrantedAuthority> authorities = new ArrayList<>();

                List<?> rawRoles = claims.get(ROLES, List.class);

                List<String> roles = rawRoles ==
                        null ? List.of() :
                        rawRoles.stream()
                                .map(String::valueOf)
                                .toList();

                roles.forEach(role ->
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));


                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                authorities
                        );

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            }


        } catch (Exception ex) {
            log.debug("JWT processing failed: {}", ex.getMessage());

            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate if the context is empty
        filterChain.doFilter(request, response);

    }

    private boolean isSwagger(String path) {
        return SWAGGER_PATH.stream().anyMatch(path::startsWith);
    }
}