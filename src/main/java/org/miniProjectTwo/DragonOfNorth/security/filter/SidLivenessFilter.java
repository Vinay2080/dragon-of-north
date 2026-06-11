package org.miniProjectTwo.DragonOfNorth.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.miniProjectTwo.DragonOfNorth.shared.util.SecurityAuditEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Enforces session-id (SID) liveness for access tokens minted with session binding.
 * <p>
 * Protects against replay of stale/stolen access tokens after server-side session revocation by
 * verifying that token SID still maps to an active session state. Assumes session persistence is
 * authoritative for revocation decisions.
 */
@Slf4j
@Component
public class SidLivenessFilter extends OncePerRequestFilter {
    private final SessionRepository sessionRepository;
    private final SidEnforcementMode enforcementMode;
    private final List<String> sensitivePatterns;
    private final AuditEventLogger auditEventLogger;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public SidLivenessFilter(
            SessionRepository sessionRepository,
            @Value("${app.security.sid-enforcement.mode:}") String enforcementMode,
            @Value("${app.security.sid-enforcement.sensitive-patterns:}") String sensitivePatterns,
            AuditEventLogger auditEventLogger
    ) {
        this.sessionRepository = sessionRepository;
        this.enforcementMode = SidEnforcementMode.from(enforcementMode);
        this.sensitivePatterns = Arrays.stream(sensitivePatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .toList();
        this.auditEventLogger = auditEventLogger;
    }

    /**
     * For applicable requests, verifies that the authenticated principal's session ID maps to a live session. If not, clears the security context to force re-authentication.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!shouldEnforce(request.getServletPath(), principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID sessionId = principal.sessionId();
        if (sessionId == null || principal.userId() == null) {
            auditEventLogger.log(SecurityAuditEvent.AUTH_SESSION_BINDING_FAILURE, principal.userId(), null, null, "failure", "sid_missing", null);
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        boolean live = sessionRepository.existsLiveSessionForUser(sessionId, principal.userId(), Instant.now());
        if (!live) {
            log.debug("SID liveness check failed for user={} sid={}", principal.userId(), sessionId);
            auditEventLogger.log(SecurityAuditEvent.AUTH_SESSION_SUSPICIOUS, principal.userId(), null, null, "failure", "sid_not_live", null);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldEnforce(String servletPath, SecurityPrincipal principal) {
        return switch (enforcementMode) {
            case DISABLED -> false;
            case ALL_AUTHENTICATED -> true;
            case MFA_ONLY -> principal.mfaVerified();
            case SENSITIVE_ONLY -> sensitivePatterns.stream()
                    .anyMatch(pattern -> PATH_MATCHER.match(pattern, servletPath));
        };
    }
}
