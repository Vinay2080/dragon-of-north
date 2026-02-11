package org.miniProjectTwo.DragonOfNorth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.EmailOtpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.EmailVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.PhoneOtpRequest;
import org.miniProjectTwo.DragonOfNorth.dto.otp.request.PhoneVerifyRequest;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.enums.OtpVerificationStatus;
import org.miniProjectTwo.DragonOfNorth.exception.ApplicationExceptionHandler;
import org.miniProjectTwo.DragonOfNorth.impl.otp.OtpService;
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
class OtpControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    @InjectMocks
    private OtpController otpController;

    @Mock
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(otpController)
                .setControllerAdvice(new ApplicationExceptionHandler())
                .build();
    }

    @Test
    void requestEmailOtp_shouldReturnCreated() throws Exception {
        // arrange
        EmailOtpRequest request = new EmailOtpRequest("test@example.com", OtpPurpose.SIGNUP);

        // act + assert
        mockMvc.perform(post("/api/v1/otp/email/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(otpService).createEmailOtp(request.email(), request.otpPurpose());
    }

    @Test
    void requestPhoneOtp_shouldReturnCreated() throws Exception {
        // arrange
        PhoneOtpRequest request = new PhoneOtpRequest("9876543210", OtpPurpose.LOGIN);

        // act + assert
        mockMvc.perform(post("/api/v1/otp/phone/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"));

        verify(otpService).createPhoneOtp(request.phone(), request.otpPurpose());
    }

    @Test
    void verifyEmailOtp_shouldReturnAccepted_whenSuccess() throws Exception {
        // arrange
        EmailVerifyRequest request = new EmailVerifyRequest("test@example.com", "123456", OtpPurpose.SIGNUP);
        when(otpService.verifyEmailOtp(request.email(), request.otp(), request.otpPurpose()))
                .thenReturn(OtpVerificationStatus.SUCCESS);

        // act + assert
        mockMvc.perform(post("/api/v1/otp/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data").value("SUCCESS"));

        verify(otpService).verifyEmailOtp(request.email(), request.otp(), request.otpPurpose());
    }

    @Test
    void verifyEmailOtp_shouldReturnBadRequest_whenFailure() throws Exception {
        // arrange
        EmailVerifyRequest request = new EmailVerifyRequest("test@example.com", "123456", OtpPurpose.SIGNUP);
        when(otpService.verifyEmailOtp(request.email(), request.otp(), request.otpPurpose()))
                .thenReturn(OtpVerificationStatus.INVALID_OTP);

        // act + assert
        mockMvc.perform(post("/api/v1/otp/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiResponseStatus").value("failed"))
                .andExpect(jsonPath("$.data").value("INVALID_OTP"));

        verify(otpService).verifyEmailOtp(request.email(), request.otp(), request.otpPurpose());
    }

    @Test
    void verifyPhoneOtp_shouldReturnAccepted_whenSuccess() throws Exception {
        // arrange
        PhoneVerifyRequest request = new PhoneVerifyRequest("9876543210", "123456", OtpPurpose.LOGIN);
        when(otpService.verifyPhoneOtp(request.phone(), request.otp(), request.otpPurpose()))
                .thenReturn(OtpVerificationStatus.SUCCESS);

        // act + assert
        mockMvc.perform(post("/api/v1/otp/phone/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.apiResponseStatus").value("success"))
                .andExpect(jsonPath("$.data").value("SUCCESS"));

        verify(otpService).verifyPhoneOtp(request.phone(), request.otp(), request.otpPurpose());
    }

    @Test
    void verifyPhoneOtp_shouldReturnBadRequest_whenFailure() throws Exception {
        // arrange
        PhoneVerifyRequest request = new PhoneVerifyRequest("9876543210", "123456", OtpPurpose.LOGIN);
        when(otpService.verifyPhoneOtp(request.phone(), request.otp(), request.otpPurpose()))
                .thenReturn(OtpVerificationStatus.EXPIRED_OTP);

        // act + assert
        mockMvc.perform(post("/api/v1/otp/phone/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiResponseStatus").value("failed"))
                .andExpect(jsonPath("$.data").value("EXPIRED_OTP"));

        verify(otpService).verifyPhoneOtp(request.phone(), request.otp(), request.otpPurpose());
    }
}
