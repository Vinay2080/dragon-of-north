package org.miniProjectTwo.DragonOfNorth.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Disabled
class OtpFlowIT extends BaseIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void shouldRequestAndVerifyEmailOtpEndToEnd() throws Exception {
        AtomicReference<String> capturedOtp = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedOtp.set(invocation.getArgument(1, String.class));
            return null;
        }).when(emailOtpSender).send(anyString(), anyString(), anyInt());

        String email = "otp-it@example.com";

        var requestPayload = new java.util.LinkedHashMap<String, Object>();
        requestPayload.put("email", email);
        requestPayload.put("otp_purpose", OtpPurpose.SIGNUP.name());

        mockMvc.perform(post("/api/v1/otp/email/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPayload)))
                .andExpect(status().isCreated());

        Mockito.verify(emailOtpSender).send(eq(email), anyString(), anyInt());

        String otp = capturedOtp.get();
        assertThat(otp).isNotBlank();

        var verifyPayload = new java.util.LinkedHashMap<String, Object>();
        verifyPayload.put("email", email);
        verifyPayload.put("otp", otp);
        verifyPayload.put("otp_purpose", OtpPurpose.SIGNUP.name());

        mockMvc.perform(post("/api/v1/otp/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyPayload)))
                .andExpect(status().isAccepted());
    }
}
