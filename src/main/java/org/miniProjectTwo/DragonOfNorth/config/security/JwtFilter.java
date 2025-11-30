package org.miniProjectTwo.DragonOfNorth.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
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
 * JWT Authentication Filter that processes JWT tokens in the Authorization header.
 * <p>
 * This filter intercepts incoming requests and performs the following actions:
 * <ol>
 *   <li>Allows public endpoints to pass through without authentication</li>
 *   <li>Extracts the JWT token from the Authorization header</li>
 *   <li>Validates the token and extracts user information</li>
 *   <li>Sets up the Spring Security context with the authenticated user</li>
 * </ol>
 *
 * <p>Public endpoints that bypass JWT validation:
 * <ul>
 *   <li>{@code /api/v1/auth/login}</li>
 *   <li>{@code /api/v1/auth/register}</li>
 *   <li>{@code /api/v1/auth/refresh/token}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtServices jwtServices; // todo create class jwtServices
    private final UserDetailsService userDetailsService; // todo implement UserDetailService


    /**
     * Processes each HTTP request to validate a JWT token.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs during request processing
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        //todo configure paths
        final String path = request.getServletPath();
        if ("/api/v1/auth/login".equals(path)
                || "/api/v1/auth/register".equals(path)
                || "/api/v1/auth/refresh/token".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        // todo add try catch block
        username = jwtServices.extractUsername(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            final UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtServices.isTokenValid(jwt, userDetails)) {
                final UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            }

        }

        filterChain.doFilter(request, response);

    }
}
