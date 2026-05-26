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
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.security.model.SecurityPrincipal;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.ApplicationExceptionHandler;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
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

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.standaloneSetup(stepUpController)
                .setControllerAdvice(new ApplicationExceptionHandler())
                .build();
    }

    // ------------------------------------------------------------------ /mfa/request

    @Test
    void requestStepUp_shouldReturnChallenge_whenUserAuthenticated() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser();
        user.setId(userId);

        MfaChallenge challenge = new MfaChallenge("token-abc", Instant.now().plusSeconds(120), List.of(ProviderType.TOTP));
        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(authCommonServices.issueStepUpChallenge(eq(user), any())).thenReturn(challenge);

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.challengeId").value("token-abc"))
                .andExpect(jsonPath("$.data.mfaRequired").value(true));

        verify(authCommonServices).findAuthenticatedUser();
        verify(authCommonServices).issueStepUpChallenge(eq(user), any());
    }

    @Test
    void requestStepUp_shouldReturn403_whenNoMfaMethodAvailable() throws Exception {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());

        when(authCommonServices.findAuthenticatedUser()).thenReturn(user);
        when(authCommonServices.issueStepUpChallenge(eq(user), any()))
                .thenThrow(new BusinessException(ErrorCode.MFA_REQUIRED, "No MFA methods available for step-up"));

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/mfa/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
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
        setUpSecurityContextWithSessionId(UUID.randomUUID(), recentlyVerified);

        when(recentMfaProperties.getMfaMaxAge()).thenReturn(Duration.ofMinutes(15));
        doNothing().when(recentMfaService).requireRecentMfa(eq(recentlyVerified), any());

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/protected-action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(recentMfaService).requireRecentMfa(eq(recentlyVerified), any());
    }

    @Test
    void sensitiveAction_shouldReturn403_whenRecentMfaIsExpired() throws Exception {
        Instant staleVerification = Instant.now().minus(Duration.ofMinutes(20));
        setUpSecurityContextWithSessionId(UUID.randomUUID(), staleVerification);

        when(recentMfaProperties.getMfaMaxAge()).thenReturn(Duration.ofMinutes(15));
        doThrow(new BusinessException(ErrorCode.MFA_STEP_UP_REQUIRED))
                .when(recentMfaService).requireRecentMfa(eq(staleVerification), any());

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/protected-action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sensitiveAction_shouldReturn403_whenMfaVerifiedAtIsNull() throws Exception {
        setUpSecurityContextWithSessionId(UUID.randomUUID(), null);

        when(recentMfaProperties.getMfaMaxAge()).thenReturn(Duration.ofMinutes(15));
        doThrow(new BusinessException(ErrorCode.MFA_STEP_UP_REQUIRED))
                .when(recentMfaService).requireRecentMfa(eq(null), any());

        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/step-up/protected-action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ helpers

    private void setUpSecurityContextWithSessionId(UUID sessionId, Instant mfaVerifiedAt) {
        SecurityPrincipal principal = new SecurityPrincipal(
                UUID.randomUUID(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                mfaVerifiedAt != null,
                mfaVerifiedAt,
                sessionId,
                List.of("pwd")
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
