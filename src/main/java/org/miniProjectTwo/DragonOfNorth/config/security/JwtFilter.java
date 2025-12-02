package org.miniProjectTwo.DragonOfNorth.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
 * <p>Any invalid or expired token results only in a silent skip — no 500 errors —
 * allowing downstream exception handlers or access rules to handle unauthorized access.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtServices jwtServices;
    private final UserDetailsService userDetailsService;

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
        String username;

        try {
            username = jwtServices.extractUsername(token);
            log.debug("Extracted username '{}' from JWT", username);
        } catch (Exception ex) {
            log.warn("Failed to parse or extract data from JWT: {}", ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate if the context is empty
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtServices.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                log.debug("Security context set for user: {}", username);
            } else {
                log.warn("Invalid JWT token for user: {}", username);
            }
        }

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
                || "/api/v1/auth/refresh/token".equals(path);
    }
}
