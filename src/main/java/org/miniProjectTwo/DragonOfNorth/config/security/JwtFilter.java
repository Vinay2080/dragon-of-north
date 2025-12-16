package org.miniProjectTwo.DragonOfNorth.config.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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


    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String path = request.getServletPath();

        // Public endpoints do not require JWT
        if (isPublic(path)) {
            log.debug("Skipping JWT filter for public path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found in request for path: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {

            if (!jwtServices.isTokenValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = jwtServices.extractAllClaims(token);
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
            log.warn("Failed to parse or extract data from JWT: {}", ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate if the context is empty
        filterChain.doFilter(request, response);

    }

    /**
     * Determines whether a servlet path should bypass JWT authentication.
     *
     * @param path the request path
     * @return true if the path is publicly accessible
     */
    private boolean isPublic(String path) {
        return "/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/refresh/token".equals(path)
                || "/api/v1/otp/email/request".equals(path)
                || "/api/v1/otp/email/verify".equals(path)
                || "/api/v1/otp/phone/request".equals(path)
                || "/api/v1/otp/phone/verify".equals(path);
    }

}
// todo javadoc