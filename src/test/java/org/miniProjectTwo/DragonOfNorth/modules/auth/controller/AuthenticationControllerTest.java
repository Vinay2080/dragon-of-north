package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.*;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupConfirmResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.MfaSetupResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.challenge.model.VerificationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.mfa.orchestrator.MfaOrchestrationResult;
import org.miniProjectTwo.DragonOfNorth.modules.auth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.*;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ProviderType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.ApplicationExceptionHandler;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuthenticationController authenticationController;

    @Mock
    private AuthenticationServiceResolver resolver;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AuthCommonServices authCommonServices;

    @Mock
    private PasswordService passwordService;

    @Mock
    private MfaService mfaService;

    @Mock
    private PasswordlessLoginService passwordlessLoginService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController)
                .setControllerAdvice(new ApplicationExceptionHandler())
                .build();
    }

    @Test
    void findUserStatus_shouldReturnStatus_whenRequestIsValid() throws Exception {
        // arrange
        AppUserStatusFinderRequest request = new AppUserStatusFinderRequest("test@example.com", IdentifierType.EMAIL);
        AppUserStatusFinderResponse response = new AppUserStatusFinderResponse(true, List.of(), false, AppUserStatus.ACTIVE);

        when(resolver.resolve(request.identifier(), request.identifierType())).thenReturn(authenticationService);
        when(authenticationService.getUserStatus(request.identifier())).thenReturn(response);

        // act & assert
        mockMvc.perform(post("/api/v1/auth/identifier/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.appUserStatus").value("ACTIVE"));

        verify(resolver).resolve(request.identifier(), request.identifierType());
        verify(authenticationService).getUserStatus(request.identifier());
    }



    @Test
    void completeUserSignup_shouldReturnACTIVE_whenRequestIsValid() throws Exception {
        // arrange
        AppUserSignUpCompleteRequest request = new AppUserSignUpCompleteRequest("test@example.com", IdentifierType.EMAIL);
        AppUserStatusFinderResponse response = new AppUserStatusFinderResponse(true, List.of(), true, AppUserStatus.ACTIVE);

        when(resolver.resolve(request.identifier(), request.identifierType())).thenReturn(authenticationService);
        when(authenticationService.completeSignUp(request.identifier())).thenReturn(response);

        // act & assert
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.appUserStatus").value("ACTIVE"));

        verify(resolver).resolve(request.identifier(), request.identifierType());
        verify(authenticationService).completeSignUp(request.identifier());
    }

    @Test
    void requestPasswordResetOtp_shouldReturnOk_whenRequestIsValid() throws Exception {
        PasswordResetRequestOtpRequest request = new PasswordResetRequestOtpRequest("test@example.com", IdentifierType.EMAIL);

        mockMvc.perform(post("/api/v1/auth/password/forgot/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(passwordService).requestPasswordResetOtp("test@example.com", IdentifierType.EMAIL);
    }

    @Test
    void resetPassword_shouldReturnOk_whenRequestIsValid() throws Exception {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest(
                "test@example.com",
                IdentifierType.EMAIL,
                "123456",
                "NewPass@123"
        );

        mockMvc.perform(post("/api/v1/auth/password/forgot/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(passwordService).resetPassword(request);
    }

    @Test
    void changePassword_shouldReturnOk_whenCamelCasePayloadIsProvided() throws Exception {
        String payload = """
                {
                  "oldPassword": "OldPass@123",
                  "newPassword": "NewPass@123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(passwordService).changePassword(new PasswordChangeRequest("OldPass@123", "NewPass@123"));
    }

    @Test
    void changePassword_shouldReturnOk_whenSnakeCasePayloadIsProvided() throws Exception {
        String payload = """
                {
                  "old_password": "OldPass@123",
                  "new_password": "NewPass@123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(passwordService).changePassword(new PasswordChangeRequest("OldPass@123", "NewPass@123"));
    }

    @Test
    void loginUser_shouldReturnUnauthorized_whenEmailNotVerified() throws Exception {
        AppUserLoginRequest request = new AppUserLoginRequest("test@example.com", "Secret@123", "device-1");
        when(authCommonServices.login(eq(request.identifier()), eq(request.password()), any(), any(AuthRequestContext.class)))
                .thenThrow(new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "Email not verified. Please verify your email before logging in."));

        mockMvc.perform(post("/api/v1/auth/identifier/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.apiResponseStatus").value("failed"))
                .andExpect(jsonPath("$.data.code").value("VAL_002"));

        verify(authCommonServices).login(eq(request.identifier()), eq(request.password()), any(), any(AuthRequestContext.class));
    }

    @Test
    void deleteAccount_shouldReturnOk_whenRequestIsValid() throws Exception {
        DeviceIdRequest request = new DeviceIdRequest("device-1");

        mockMvc.perform(post("/api/v1/auth/account/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(authCommonServices).deleteAccount(any(), any(AuthRequestContext.class));
    }

    @Test
    void verifyPasswordlessLogin_shouldAcceptTokenAndDeviceIdFromJsonBody() throws Exception {
        String payload = """
                {
                  "token": "raw-token",
                  "device_id": "device-1"
                }
                """;
        when(passwordlessLoginService.verifyPasswordlessLogin(eq("raw-token"), any(AuthRequestContext.class), any()))
                .thenReturn(MfaOrchestrationResult.noChallenge(false, List.of()));

        mockMvc.perform(post("/api/v1/auth/passwordless/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(passwordlessLoginService).verifyPasswordlessLogin(
                eq("raw-token"),
                argThat(context -> "device-1".equals(context.deviceId())),
                any()
        );
    }

    @Test
    void verifyPasswordlessLogin_shouldAcceptLoginScopedAlias() throws Exception {
        String payload = """
                {
                  "token": "raw-token",
                  "device_id": "device-1"
                }
                """;
        when(passwordlessLoginService.verifyPasswordlessLogin(eq("raw-token"), any(AuthRequestContext.class), any()))
                .thenReturn(MfaOrchestrationResult.noChallenge(false, List.of()));

        mockMvc.perform(post("/api/v1/auth/login/passwordless/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(passwordlessLoginService).verifyPasswordlessLogin(
                eq("raw-token"),
                argThat(context -> "device-1".equals(context.deviceId())),
                any()
        );
    }

    @Test
    void verifyPasswordlessLogin_shouldReturnBadRequest_whenDeviceIdMissing() throws Exception {
        String payload = """
                {
                  "token": "raw-token"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/passwordless/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiResponseStatus").value("failed"))
                .andExpect(jsonPath("$.data.code").value("VAL_001"));

        verify(passwordlessLoginService, never()).verifyPasswordlessLogin(anyString(), any(), any());
    }

    @Test
    void requestMfaSetup_shouldReturnOk_whenRequestIsValid() throws Exception {
        // arrange
        DeviceIdRequest request = new DeviceIdRequest("device-1");
        when(mfaService.requestMfaSetup(any(AuthRequestContext.class)))
                .thenReturn(new MfaSetupResponse("secret-123", "data:image/png;base64,AAAA"));

        // act + assert
        mockMvc.perform(post("/api/v1/auth/enable/mfa/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.mfaSecret").value("secret-123"))
                .andExpect(jsonPath("$.data.mfaQrCode").value("data:image/png;base64,AAAA"));

        verify(mfaService).requestMfaSetup(argThat(context -> "device-1".equals(context.deviceId())));
    }

    @Test
    void verifyMfaChallenge_shouldForwardChallengePayload() throws Exception {
        String payload = """
                {
                  "challenge_id": "challenge-1",
                  "provider_type": "TOTP",
                  "code": "123456",
                  "device_id": "device-1"
                }
                """;

        when(authCommonServices.completeMfaChallengeLogin(
                eq("challenge-1"),
                eq("123456"),
                eq(ProviderType.TOTP),
                any(),
                any(AuthRequestContext.class)
        )).thenReturn(VerificationResult.success(UUID.randomUUID(), "password", "totp"));

        mockMvc.perform(post("/api/v1/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(authCommonServices).completeMfaChallengeLogin(
                eq("challenge-1"),
                eq("123456"),
                eq(ProviderType.TOTP),
                any(),
                argThat(context -> "device-1".equals(context.deviceId()))
        );
    }

    @Test
    void requestMfaSetup_shouldAcceptSnakeCaseDeviceId() throws Exception {
        // arrange
        when(mfaService.requestMfaSetup(any(AuthRequestContext.class)))
                .thenReturn(new MfaSetupResponse("secret-123", "data:image/png;base64,AAAA"));

        String payload = """
                {
                  "device_id": "device-1"
                }
                """;

        // act + assert
        mockMvc.perform(post("/api/v1/auth/enable/mfa/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(mfaService).requestMfaSetup(argThat(context -> "device-1".equals(context.deviceId())));
    }

    @Test
    void confirmMfaSetup_shouldReturnOk_whenRequestIsValid() throws Exception {
        // arrange
        MfaSetupConfirmRequest request = new MfaSetupConfirmRequest("123456", "device-1");
        when(mfaService.confirmMfaSetup(any(AuthRequestContext.class), eq("123456")))
                .thenReturn(new MfaSetupConfirmResponse(new String[]{"code-1", "code-2"}));

        // act + assert
        mockMvc.perform(post("/api/v1/auth/enable/mfa/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.backupCodes[0]").value("code-1"))
                .andExpect(jsonPath("$.data.backupCodes[1]").value("code-2"));

        verify(mfaService).confirmMfaSetup(
                argThat(context -> "device-1".equals(context.deviceId())),
                eq("123456")
        );
    }

    @Test
    void confirmMfaSetup_shouldAcceptOtpAliasAndSnakeCaseDeviceId() throws Exception {
        // arrange
        when(mfaService.confirmMfaSetup(any(AuthRequestContext.class), eq("123456")))
                .thenReturn(new MfaSetupConfirmResponse(new String[]{"code-1"}));

        String payload = """
                {
                  "otp": "123456",
                  "device_id": "device-1"
                }
                """;

        // act + assert
        mockMvc.perform(post("/api/v1/auth/enable/mfa/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.backupCodes[0]").value("code-1"));

        verify(mfaService).confirmMfaSetup(
                argThat(context -> "device-1".equals(context.deviceId())),
                eq("123456")
        );
    }

    @Test
    void confirmMfaSetup_shouldReturnBadRequest_whenDeviceIdMissing() throws Exception {
        String payload = """
                {
                  "code": "123456"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/enable/mfa/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiResponseStatus").value("failed"))
                .andExpect(jsonPath("$.data.code").value("VAL_001"));

        verify(mfaService, never()).confirmMfaSetup(any(AuthRequestContext.class), anyString());
    }

}
