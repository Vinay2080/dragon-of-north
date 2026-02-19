package org.miniProjectTwo.DragonOfNorth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserSignUpCompleteRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.request.AppUserStatusFinderRequest;
import org.miniProjectTwo.DragonOfNorth.dto.auth.response.AppUserStatusFinderResponse;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.exception.ApplicationExceptionHandler;
import org.miniProjectTwo.DragonOfNorth.resolver.AuthenticationServiceResolver;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.AuthenticationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    //todo login method test
    //todo logout method test

}
