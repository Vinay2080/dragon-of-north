package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.DeviceIdRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.MfaVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.MfaChallenge;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaProperties;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.stepup.RecentMfaService;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.MfaService;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.security.service.SessionAccessTokenIssuer;
import org.miniProjectTwo.DragonOfNorth.security.web.RecentMfaEnforcementInterceptor;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.ApplicationExceptionHandler;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.shared.util.AuditEventLogger;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StepUpControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private StepUpController stepUpController;

    @Mock
    private AuthCommonServices authCommonServices;

    @Mock
    private RecentMfaService recentMfaService;

    @Mock
    private RecentMfaProperties recentMfaProperties;

    @Mock
    private AuditEventLogger auditEventLogger;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SessionAccessTokenIssuer sessionAccessTokenIssuer;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MfaService mfaService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        RecentMfaEnforcementInterceptor recentMfaEnforcementInterceptor = new RecentMfaEnforcementInterceptor(
                recentMfaService,
                recentMfaProperties,
                auditEventLogger,
                appUserRepository,
                sessionRepository,
                sessionAccessTokenIssuer,
                roleRepository,
                authCommonServices,
                objectMapper
        );
        mockMvc = MockMvcBuilders.standaloneSetup(stepUpController)
                .setControllerAdvice(new ApplicationExceptionHandler())
                .addInterceptors(recentMfaEnforcementInterceptor)
                .build();
    }

    // ------------------------------------------------------------------ /mfa/request

    @Test
    void requestStepUp_shouldReturnChallenge_whenUserAuthenticated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);
        setUpSecurityContextWithSessionId(sessionId, Instant.now());

        MfaChallenge challenge = new MfaChallenge("token-abc", Instant.now().plusSeconds(120), List.of(ProviderType.TOTP));
        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(authCommonServices.issueStepUpChallenge(eq(user), eq(sessionId), any())).thenReturn(challenge);

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.challengeId").value("token-abc"))
                .andExpect(jsonPath("$.data.mfaRequired").value(true));

        verify(authCommonServices).findAuthenticatedUser();
        verify(authCommonServices).issueStepUpChallenge(eq(user), eq(sessionId), any());
    }

    @Test
    void requestStepUp_shouldReturn403_whenNoMfaMethodAvailable() throws Exception {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        UUID sessionId = UUID.randomUUID();
        setUpSecurityContextWithSessionId(sessionId, Instant.now());

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(authCommonServices.issueStepUpChallenge(eq(user), eq(sessionId), any()))
                .thenThrow(new BusinessException(ErrorCode.MFA_REQUIRED, "No MFA methods available for step-up"));

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.apiResponseStatus").value("failed"))
                .andExpect(jsonPath("$.data.code").value(ErrorCode.MFA_REQUIRED.getCode()));
    }

    // ------------------------------------------------------------------ /mfa/verify

    @Test
    void verifyStepUp_shouldReturn200_whenChallengeSucceeds() throws Exception {
        UUID sessionId = UUID.randomUUID();
        setUpSecurityContextWithSessionId(sessionId, Instant.now());

        MfaVerifyRequest request = new MfaVerifyRequest("challenge-123", ProviderType.TOTP, "123456", "device-1");

        doNothing().when(authCommonServices).completeStepUpMfaChallenge(any(), any(), any(), eq(sessionId), any(), any());

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(authCommonServices).completeStepUpMfaChallenge(
                eq("challenge-123"), eq(ProviderType.TOTP), eq("123456"), eq(sessionId), any(), any());
    }

    @Test
    void verifyStepUp_shouldReturn4xx_whenChallengeVerificationFails() throws Exception {
        UUID sessionId = UUID.randomUUID();
        setUpSecurityContextWithSessionId(sessionId, Instant.now());

        MfaVerifyRequest request = new MfaVerifyRequest("challenge-bad", ProviderType.TOTP, "000000", "device-1");

        doThrow(new BusinessException(ErrorCode.MFA_INVALID_CODE))
                .when(authCommonServices).completeStepUpMfaChallenge(any(), any(), any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void verifyStepUp_shouldReturn4xx_whenChallengeReplayAttempted() throws Exception {
        UUID sessionId = UUID.randomUUID();
        setUpSecurityContextWithSessionId(sessionId, Instant.now());

        MfaVerifyRequest request = new MfaVerifyRequest("challenge-replayed", ProviderType.TOTP, "111111", "device-1");

        doThrow(new BusinessException(ErrorCode.MFA_CHALLENGE_FAILED))
                .when(authCommonServices).completeStepUpMfaChallenge(any(), any(), any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ------------------------------------------------------------------ /protected-action

    @Test
    void sensitiveAction_shouldReturn200_whenRecentMfaIsFresh() throws Exception {
        Instant recentlyVerified = Instant.now().minusSeconds(30);
        UUID sessionId = UUID.randomUUID();
        UUID userId = setUpSecurityContextWithSessionId(sessionId, recentlyVerified);
        AppUser user = new AppUser();
        user.setId(userId);
        user.setMfaEnabled(true);
        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        Session session = buildSession(sessionId, recentlyVerified);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(java.util.Optional.of(session));

        when(recentMfaProperties.resolveMaxAge(any())).thenReturn(Duration.ofMinutes(15));
        when(recentMfaService.isRecentMfaSatisfied(eq(recentlyVerified), any())).thenReturn(true);

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/protected-action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(recentMfaService, never()).requireRecentMfa(eq(recentlyVerified), any());
    }

    @Test
    void sensitiveAction_shouldReturn403_whenRecentMfaIsExpired() throws Exception {
        Instant staleVerification = Instant.now().minus(Duration.ofMinutes(20));
        UUID sessionId = UUID.randomUUID();
        UUID userId = setUpSecurityContextWithSessionId(sessionId, staleVerification);
        AppUser user = new AppUser();
        user.setId(userId);
        user.setMfaEnabled(true);
        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        Session session = buildSession(sessionId, staleVerification);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(java.util.Optional.of(session));

        when(recentMfaProperties.resolveMaxAge(any())).thenReturn(Duration.ofMinutes(15));
        when(recentMfaService.isRecentMfaSatisfied(eq(staleVerification), any())).thenReturn(false);
        MfaChallenge challenge = new MfaChallenge("token-expired", Instant.now().plusSeconds(120), List.of(ProviderType.TOTP));
        when(authCommonServices.issueStepUpChallenge(eq(user), eq(sessionId), any())).thenReturn(challenge);

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/protected-action")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.apiResponseStatus").value("failed"))
                .andExpect(jsonPath("$.data.code").value(ErrorCode.MFA_STEP_UP_REQUIRED.getCode()))
                .andExpect(jsonPath("$.data.challengeId").value("token-expired"))
                .andExpect(jsonPath("$.data.availableMethods[0]").value("TOTP"))
                .andExpect(jsonPath("$.data.expiresAt").exists());
        verify(authCommonServices).issueStepUpChallenge(eq(user), eq(sessionId), any());
    }

    @Test
    void sensitiveAction_shouldReturn401AndNeverIssueChallenge_whenSessionNotLive() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID userId = setUpSecurityContextWithSessionId(sessionId, Instant.now().minus(Duration.ofMinutes(25)));
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(java.util.Optional.empty());

        mockMvc.perform(post("/api/v1/auth/step-up/protected-action")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeviceIdRequest("device-1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value(ErrorCode.INVALID_TOKEN.getCode()))
                .andExpect(jsonPath("$.data.defaultMessage").value(containsString("no longer live")));

        verify(authCommonServices, never()).issueStepUpChallenge(any(), any(), any());
    }

    @Test
    void sensitiveAction_shouldPassAndRefreshAccessToken_whenSessionFreshButJwtStale() throws Exception {
        Instant jwtStale = Instant.now().minus(Duration.ofMinutes(30));
        Instant sessionFresh = Instant.now().minus(Duration.ofSeconds(20));
        UUID sessionId = UUID.randomUUID();
        UUID userId = setUpSecurityContextWithSessionId(sessionId, jwtStale);

        AppUser user = new AppUser();
        user.setId(userId);
        user.setMfaEnabled(true);
        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.of(user));

        Session session = buildSession(sessionId, sessionFresh);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(java.util.Optional.of(session));
        when(recentMfaProperties.resolveMaxAge(any())).thenReturn(Duration.ofMinutes(15));
        when(recentMfaService.isRecentMfaSatisfied(eq(jwtStale), any())).thenReturn(false);
        when(recentMfaService.isRecentMfaSatisfied(eq(sessionFresh), any())).thenReturn(true);
        when(roleRepository.findRolesById(userId)).thenReturn(java.util.Set.of());
        when(sessionAccessTokenIssuer.mintAccessToken(session, java.util.Set.of())).thenReturn("refreshed-access-token");

        mockMvc.perform(post("/api/v1/auth/step-up/protected-action")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeviceIdRequest("device-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(sessionAccessTokenIssuer).mintAccessToken(session, java.util.Set.of());
        verify(authCommonServices).setAccessToken(any(), eq("refreshed-access-token"));
        verify(authCommonServices, never()).issueStepUpChallenge(any(), any(), any());
    }

    @Test
    void sensitiveAction_shouldReturn403_whenMfaVerifiedAtIsNull() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID userId = setUpSecurityContextWithSessionId(sessionId, null);
        AppUser user = new AppUser();
        user.setId(userId);
        user.setMfaEnabled(true);
        when(appUserRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        Session session = buildSession(sessionId, null);
        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any())).thenReturn(java.util.Optional.of(session));

        when(recentMfaProperties.resolveMaxAge(any())).thenReturn(Duration.ofMinutes(15));
        when(recentMfaService.isRecentMfaSatisfied(eq(null), any())).thenReturn(false);
        MfaChallenge challenge = new MfaChallenge("token-missing", Instant.now().plusSeconds(120), List.of(ProviderType.TOTP));
        when(authCommonServices.issueStepUpChallenge(eq(user), eq(sessionId), any())).thenReturn(challenge);

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/protected-action")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        verify(authCommonServices).issueStepUpChallenge(eq(user), eq(sessionId), any());
    }

    @Test
    void disableMfa_shouldReturn403_whenRecentMfaExpired() throws Exception {
        Instant staleVerification = Instant.now().minus(Duration.ofMinutes(20));

        UUID sessionId = UUID.randomUUID();
        UUID userId = setUpSecurityContextWithSessionId(sessionId, staleVerification);

        AppUser user = new AppUser();
        user.setId(userId);
        user.setMfaEnabled(true);

        when(appUserRepository.findById(userId))
                .thenReturn(java.util.Optional.of(user));

        Session session = buildSession(sessionId, staleVerification);

        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any()))
                .thenReturn(java.util.Optional.of(session));

        when(recentMfaProperties.resolveMaxAge(any()))
                .thenReturn(Duration.ofMinutes(15));

        when(recentMfaService.isRecentMfaSatisfied(eq(staleVerification), any()))
                .thenReturn(false);

        MfaChallenge challenge = new MfaChallenge(
                "disable-mfa-challenge",
                Instant.now().plusSeconds(120),
                List.of(ProviderType.TOTP)
        );

        when(authCommonServices.issueStepUpChallenge(eq(user), eq(sessionId), any()))
                .thenReturn(challenge);

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/disable")
                        .header("X-Device-Id", "device-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeviceIdRequest("device-1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.code")
                        .value(ErrorCode.MFA_STEP_UP_REQUIRED.getCode()));

        verify(mfaService, never()).disableMfa(any());
    }

    @Test
    void disableMfa_shouldReturn200_whenRecentMfaSatisfied() throws Exception {
        Instant recentlyVerified = Instant.now().minusSeconds(30);

        UUID sessionId = UUID.randomUUID();
        UUID userId = setUpSecurityContextWithSessionId(sessionId, recentlyVerified);

        AppUser user = new AppUser();
        user.setId(userId);
        user.setMfaEnabled(true);

        when(appUserRepository.findById(userId))
                .thenReturn(java.util.Optional.of(user));

        Session session = buildSession(sessionId, recentlyVerified);

        when(sessionRepository.findLiveByIdAndAppUserId(eq(sessionId), eq(userId), any()))
                .thenReturn(java.util.Optional.of(session));

        when(recentMfaProperties.resolveMaxAge(any()))
                .thenReturn(Duration.ofMinutes(15));

        when(recentMfaService.isRecentMfaSatisfied(eq(recentlyVerified), any()))
                .thenReturn(true);

        doNothing().when(mfaService).disableMfa(any());

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeviceIdRequest("device-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(mfaService).disableMfa(any());
    }

    @Test
    void mfaStatus_shouldReturnEnabledFalse_whenUserHasMfaDisabled() throws Exception {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setMfaEnabled(false);

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mfaEnabled").value(false));
    }

    @Test
    void mfaStatus_shouldReturnEnabledTrue_whenUserHasMfaEnabled() throws Exception {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setMfaEnabled(true);

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.mfaEnabled").value(true));

        verify(authCommonServices).findAuthenticatedUser();
    }
    // ------------------------------------------------------------------ helpers

    private UUID setUpSecurityContextWithSessionId(UUID sessionId, Instant mfaVerifiedAt) {
        UUID userId = UUID.randomUUID();
        SecurityPrincipal principal = new SecurityPrincipal(
                userId,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                mfaVerifiedAt != null,
                mfaVerifiedAt,
                sessionId,
                List.of("pwd")
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        return userId;
    }

    private Session buildSession(UUID sessionId, Instant mfaVerifiedAt) {
        Session session = new Session();
        session.setId(sessionId);
        session.setMfaVerifiedAt(mfaVerifiedAt);
        return session;
    }
}
