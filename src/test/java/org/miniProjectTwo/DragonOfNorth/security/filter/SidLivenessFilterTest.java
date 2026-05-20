package org.miniProjectTwo.DragonOfNorth.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SidLivenessFilterTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldClearAuthentication_whenModeAllAuthenticatedAndSidMissing() throws ServletException, IOException {
        SidLivenessFilter filter = new SidLivenessFilter(sessionRepository, "all-authenticated", "/api/v1/session/**");
        SecurityPrincipal principal = new SecurityPrincipal(UUID.randomUUID(), List.of(new SimpleGrantedAuthority("ROLE_USER")), false, null, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");

        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldClearAuthentication_whenSidRevokedOrMissingInStore() throws ServletException, IOException {
        SidLivenessFilter filter = new SidLivenessFilter(sessionRepository, "all-authenticated", "/api/v1/session/**");
        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(new SimpleGrantedAuthority("ROLE_USER")), true, Instant.now(), sid, List.of("pwd"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
        when(sessionRepository.existsLiveSessionForUser(eq(sid), eq(userId), any(Instant.class))).thenReturn(false);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldPreserveAuthentication_whenModeSensitiveOnlyAndNonSensitivePath() throws ServletException, IOException {
        SidLivenessFilter filter = new SidLivenessFilter(sessionRepository, "sensitive-only", "/api/v1/session/**");
        UUID userId = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        SecurityPrincipal principal = new SecurityPrincipal(userId, List.of(new SimpleGrantedAuthority("ROLE_USER")), false, null, sid, List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/profile/me");
        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(sessionRepository, never()).existsLiveSessionForUser(any(), any(), any());
    }
}
