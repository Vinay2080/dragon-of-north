package org.miniProjectTwo.DragonOfNorth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserLoginRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpCompleteRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.exception.ApplicationExceptionHandler;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.AuthCommonServices;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.AuthenticationService;
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
        AppUserLoginRequest request = new AppUserLoginRequest("test@example.com", "password123");

        // act
        mockMvc.perform(post("/api/v1/auth/identifier/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("log in successful"));

        // assert
        verify(authCommonServices).login(eq("test@example.com"), eq("password123"), any(HttpServletResponse.class));
    }

    @Test
    void refreshToken_shouldReturnOk_whenRefreshTokenIsValid() throws Exception {
        // arrange
        doNothing().when(authCommonServices).refreshToken(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // act
        mockMvc.perform(post("/api/v1/auth/jwt/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("refresh token sent"));

        // assert
        verify(authCommonServices).refreshToken(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    void logoutUser_shouldReturnOk_whenLogoutIsSuccessful() throws Exception {
        // arrange
        doNothing().when(authCommonServices).logoutUser(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // act
        mockMvc.perform(post("/api/v1/auth/identifier/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("user logged out successfully"));

        // assert
        verify(authCommonServices).logoutUser(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }




}
