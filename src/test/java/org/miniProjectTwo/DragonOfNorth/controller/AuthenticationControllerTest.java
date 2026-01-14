package org.miniProjectTwo.DragonOfNorth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.components.SignupRateLimiter;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.*;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AuthenticationResponse;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.RefreshTokenResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.exception.ApplicationExceptionHandler;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.services.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.services.AuthenticationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuthenticationController authenticationController;

    @Mock
    private AuthenticationServiceResolver resolver;

    @Mock
    private SignupRateLimiter signupRateLimiter;

    @Mock
    private AuthCommonServices authCommonServices;

    @Mock
    private AuthenticationService authenticationService;

    private static final String VALID_PASSWORD = "Password@123";

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
        AppUserStatusFinderResponse response = new AppUserStatusFinderResponse(AppUserStatus.CREATED);

        when(resolver.resolve(request.identifier(), request.identifierType())).thenReturn(authenticationService);
        when(authenticationService.getUserStatus(request.identifier())).thenReturn(response);

        // act & assert
        mockMvc.perform(post("/api/v1/auth/identifier/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.appUserStatus").value("CREATED"));

        verify(resolver).resolve(request.identifier(), request.identifierType());
        verify(authenticationService).getUserStatus(request.identifier());
    }

    @Test
    void signupUser_shouldReturnCreated_whenRequestIsValid() throws Exception {
        // arrange
        AppUserSignUpRequest request = new AppUserSignUpRequest("test@example.com", IdentifierType.EMAIL, VALID_PASSWORD);
        AppUserStatusFinderResponse response = new AppUserStatusFinderResponse(AppUserStatus.CREATED);

        when(resolver.resolve(request.identifier(), request.identifierType())).thenReturn(authenticationService);
        when(authenticationService.signUpUser(any(AppUserSignUpRequest.class))).thenReturn(response);

        // act & assert
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up")
                        .header("X-Forwarded-For", "127.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.appUserStatus").value("CREATED"));

        verify(signupRateLimiter).check(eq("test@example.com"), eq("127.0.0.1"));
        verify(resolver).resolve(request.identifier(), request.identifierType());
        verify(authenticationService).signUpUser(any(AppUserSignUpRequest.class));
    }

    @Test
    void signupUser_shouldUseRemoteAddr_whenXForwardedForIsMissing() throws Exception {
        // arrange
        AppUserSignUpRequest request = new AppUserSignUpRequest("test@example.com", IdentifierType.EMAIL, VALID_PASSWORD);
        AppUserStatusFinderResponse response = new AppUserStatusFinderResponse(AppUserStatus.CREATED);

        when(resolver.resolve(request.identifier(), request.identifierType())).thenReturn(authenticationService);
        when(authenticationService.signUpUser(any(AppUserSignUpRequest.class))).thenReturn(response);

        // act & assert
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up")
                        .remoteAddress("192.168.1.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(signupRateLimiter).check(eq("test@example.com"), eq("192.168.1.1"));
    }

    @Test
    void completeUserSignup_shouldReturnCreated_whenRequestIsValid() throws Exception {
        // arrange
        AppUserSignUpCompleteRequest request = new AppUserSignUpCompleteRequest("test@example.com", IdentifierType.EMAIL);
        AppUserStatusFinderResponse response = new AppUserStatusFinderResponse(AppUserStatus.VERIFIED);

        when(resolver.resolve(request.identifier(), request.identifierType())).thenReturn(authenticationService);
        when(authenticationService.completeSignUp(request.identifier())).thenReturn(response);

        // act & assert
        mockMvc.perform(post("/api/v1/auth/identifier/sign-up/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.appUserStatus").value("VERIFIED"));

        verify(resolver).resolve(request.identifier(), request.identifierType());
        verify(authenticationService).completeSignUp(request.identifier());
    }

    @Test
    void loginUser_shouldReturnOk_whenCredentialsAreValid() throws Exception {
        // arrange
        AppUserLoginRequest request = new AppUserLoginRequest("test@example.com", VALID_PASSWORD);
        AuthenticationResponse response = AuthenticationResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .build();

        when(authCommonServices.login(eq(request.identifier()), eq(request.password()), any(HttpServletResponse.class)))
                .thenReturn(response);

        // act & assert
        mockMvc.perform(post("/api/v1/auth/identifier/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        verify(authCommonServices).login(eq(request.identifier()), eq(request.password()), any(HttpServletResponse.class));
    }

    @Test
    void refreshToken_shouldReturnNewTokens_whenRequestIsValid() throws Exception {
        // arrange
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken("new-access-token")
                .tokenType("Bearer")
                .build();

        when(authCommonServices.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        // act & assert
        mockMvc.perform(post("/api/v1/auth/jwt/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));

        verify(authCommonServices).refreshToken(any(RefreshTokenRequest.class));
    }
}
