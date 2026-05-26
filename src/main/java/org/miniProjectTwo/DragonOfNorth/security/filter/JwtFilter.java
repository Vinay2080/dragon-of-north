package org.miniProjectTwo.DragonOfNorth.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.miniProjectTwo.DragonOfNorth.security.config.SecurityConfig.public_urls;


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

 * Extracts JWT-based identity and populates the Spring Security context for downstream handlers.
 * <p>
 * Runs early in the filter chain, so authorization, user-state validation, rate-limit identity keys,
 * and audit logging can rely on authenticated principal data. Incorrect parsing/validation behavior
 * can lead to privilege bypass or unintended anonymous execution.
 */
@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtServices jwtServices;

    private final static String ROLES = "roles";
    private static final String MFA_VERIFIED = "mfa_verified";
    private static final String MFA_VERIFIED_AT = "mfa_verified_at";
    private static final String AMR = "amr";
    private static final String SESSION_ID = "sid";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public JwtFilter(JwtServices jwtServices) {
        this.jwtServices = jwtServices;
    }

    @Override
    public void doFilterInternal(
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
                SecurityContextHolder.clearContext();
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


                Instant mfaVerifiedAt = resolveMfaVerifiedAt(claims);
                boolean mfaVerified = resolveMfaVerified(claims, mfaVerifiedAt);
                List<String> amr = resolveAmr(claims);
                UUID sessionId = resolveSessionId(claims);

                SecurityPrincipal principal = new SecurityPrincipal(
                        userId,
                        authorities,
                        mfaVerified,
                        mfaVerifiedAt,
                        sessionId,
                        amr
                );

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                authorities
                        );

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            }

        } catch (RuntimeException ex) {
            log.debug("JWT processing failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();

            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate if the context is empty
        filterChain.doFilter(request, response);

    }

    private boolean isPublic(String path) {
        return Stream.of(public_urls).anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private boolean resolveMfaVerified(Claims claims, Instant mfaVerifiedAt) {
        boolean verifiedClaim = Boolean.TRUE.equals(claims.get(MFA_VERIFIED, Boolean.class));
        if (!verifiedClaim) {
            return false;
        }
        if (mfaVerifiedAt == null) {
            throw new IllegalArgumentException("JWT mfa_verified_at is required when mfa_verified=true");
        }
        return true;
    }

    private Instant resolveMfaVerifiedAt(Claims claims) {
        Object raw = claims.get(MFA_VERIFIED_AT);
        if (raw instanceof Date date) {
            return date.toInstant();
        }
        if (raw instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        return null;
    }

    private List<String> resolveAmr(Claims claims) {
        Object raw = claims.get(AMR);
        if (raw instanceof List<?> list) {
            List<String> amr = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    amr.add(entry.toString());
                }
            }
            if (!amr.isEmpty()) {
                return List.copyOf(amr);
            }
            return List.of();
        }
        return List.of();
    }

    private UUID resolveSessionId(Claims claims) {
        String raw = claims.get(SESSION_ID, String.class);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
