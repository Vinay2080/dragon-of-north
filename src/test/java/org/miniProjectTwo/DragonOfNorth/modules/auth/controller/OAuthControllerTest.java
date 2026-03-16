package org.miniProjectTwo.DragonOfNorth.modules.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.OAuthService;
import org.miniProjectTwo.DragonOfNorth.shared.exception.ApplicationExceptionHandler;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @Mock
    private OAuthService oAuthService;

    @InjectMocks
    private OAuthController oAuthController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(oAuthController)
                .setControllerAdvice(new ApplicationExceptionHandler())
                .build();
    }

    @Test
    void authenticateWithGoogle_shouldDelegateToServiceAndReturnOk() throws Exception {
        String payload = """
                {
                    "id_token":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "device_id":"device-101",
                    "expected_identifier":"user@example.com"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.message").value("OAuth authentication successful"));

        verify(oAuthService).authenticatedWithGoogle(
                eq("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                eq("device-101"),
                eq("user@example.com"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void signupWithGoogle_shouldDelegateToServiceAndReturnOk() throws Exception {
        String payload = objectMapper.writeValueAsString(new java.util.LinkedHashMap<>() {{
            put("id_token", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
            put("device_id", "device-777");
            put("expected_identifier", "new.user@example.com");
        }});

        mockMvc.perform(post("/api/v1/auth/oauth/google/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.message").value("OAuth signup successful"));

        verify(oAuthService).signupWithGoogle(
                eq("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
                eq("device-777"),
                eq("new.user@example.com"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void authenticateWithGoogle_shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiResponseStatus").value("failed"));

        verifyNoInteractions(oAuthService);
    }
}
