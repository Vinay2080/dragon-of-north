package org.miniProjectTwo.DragonOfNorth.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.security.service.JwtServices;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    private JwtFilter jwtFilter;

    @Mock
    private JwtServices jwtServices;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtServices, false);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldSkipForPublicPath() throws ServletException, IOException {
        // arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtServices);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldAuthenticateWithValidBearerToken() throws ServletException, IOException {
        // arrange
        UUID userId = UUID.randomUUID();
        String token = "valid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(claims.get("token_type", String.class)).thenReturn("access_token");
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("roles", List.class)).thenReturn(List.of("USER"));
        when(jwtServices.extractAllClaims(token)).thenReturn(claims);

        // act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // assert
        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertInstanceOf(SecurityPrincipal.class, principal);
        assertEquals(userId, ((SecurityPrincipal) principal).userId());
        assertFalse(((SecurityPrincipal) principal).mfaVerified());
        assertNull(((SecurityPrincipal) principal).mfaVerifiedAt());
        assertEquals(List.of(), ((SecurityPrincipal) principal).amr());
    }

    @Test
    void doFilterInternal_shouldUseLegacyFallbackOnlyWhenCompatibilityEnabled() throws Exception {
        JwtFilter legacyModeFilter = new JwtFilter(jwtServices, true);
        UUID userId = UUID.randomUUID();
        String token = "legacy-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(claims.get("token_type", String.class)).thenReturn("access_token");
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("roles", List.class)).thenReturn(List.of("USER"));
        when(jwtServices.extractAllClaims(token)).thenReturn(claims);

        Instant before = Instant.now();
        legacyModeFilter.doFilterInternal(request, response, filterChain);
        Instant after = Instant.now();

        SecurityPrincipal principal = (SecurityPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertTrue(principal.mfaVerified());
        assertNotNull(principal.mfaVerifiedAt());
        assertFalse(principal.mfaVerifiedAt().isBefore(before));
        assertFalse(principal.mfaVerifiedAt().isAfter(after));
        assertEquals(List.of("legacy_pwd"), principal.amr());
    }

    @Test
    void doFilterInternal_shouldAuthenticateWithValidCookieToken() throws ServletException, IOException {
        // arrange
        UUID userId = UUID.randomUUID();
        String token = "valid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        request.setCookies(new Cookie("access_token", token));
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(claims.get("token_type", String.class)).thenReturn("access_token");
        when(claims.getSubject()).thenReturn(userId.toString());
        when(claims.get("roles", List.class)).thenReturn(List.of("USER"));
        when(jwtServices.extractAllClaims(token)).thenReturn(claims);

        // act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // assert
        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertInstanceOf(SecurityPrincipal.class, principal);
        assertEquals(userId, ((SecurityPrincipal) principal).userId());
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateIfTokenMissing() throws ServletException, IOException {
        // arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtServices);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateIfTokenIsRefreshToken() throws ServletException, IOException {
        // arrange
        String token = "refresh-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(claims.get("token_type", String.class)).thenReturn("refresh_token");
        when(jwtServices.extractAllClaims(token)).thenReturn(claims);

        // act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldNotAuthenticateIfExtractionFails() throws ServletException, IOException {
        // arrange
        String token = "invalid-token";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtServices.extractAllClaims(token)).thenThrow(new RuntimeException("Invalid token"));

        // act
        jwtFilter.doFilterInternal(request, response, filterChain);

        // assert
        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
