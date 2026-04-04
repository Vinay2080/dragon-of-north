package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.request.*;
import org.miniProjectTwo.DragonOfNorth.modules.auth.dto.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.modules.auth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.AuthenticationService;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.exception.ApplicationExceptionHandler;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

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

        verify(authCommonServices).requestPasswordResetOtp("test@example.com", IdentifierType.EMAIL);
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

        verify(authCommonServices).resetPassword(request);
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

        verify(authCommonServices).changePassword(new PasswordChangeRequest("OldPass@123", "NewPass@123"));
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

        verify(authCommonServices).changePassword(new PasswordChangeRequest("OldPass@123", "NewPass@123"));
    }

    @Test
    void loginUser_shouldReturnUnauthorized_whenEmailNotVerified() throws Exception {
        AppUserLoginRequest request = new AppUserLoginRequest("test@example.com", "Secret@123", "device-1");
        doThrow(new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "Email not verified. Please verify your email before logging in."))
                .when(authCommonServices)
                .login(eq(request.identifier()), eq(request.password()), any(), any(AuthRequestContext.class));

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


}
