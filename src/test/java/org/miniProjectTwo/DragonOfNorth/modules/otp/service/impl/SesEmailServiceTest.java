package org.miniProjectTwo.DragonOfNorth.modules.otp.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SesEmailServiceTest {

    private static final String SENDER = "noreply@example.com";
    private static final String DISPLAY_SOURCE = "Dragon of North <noreply@example.com>";

    @InjectMocks
    private SesEmailService sesEmailService;

    @Mock
    private SesClient sesClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sesEmailService, "sender", SENDER);
    }

    @Test
    void sendOtpEmail_shouldCallSesClientWithCorrectRequest() {
        // arrange
        String to = "user@example.com";
        String otp = "123456";
        int ttlMinutes = 5;

        // act
        sesEmailService.sendOtpEmail(to, otp, ttlMinutes);

        // verify
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());

        SendEmailRequest request = captor.getValue();
        assertEquals(DISPLAY_SOURCE, request.source());
        assertEquals(to, request.destination().toAddresses().getFirst());
        assertTrue(request.message().body().text().data().contains(otp.substring(0, 3) + " " + otp.substring(3)));
        assertTrue(request.message().body().text().data().contains(String.valueOf(ttlMinutes)));
        assertTrue(request.message().body().html().data().contains("Dragon of North"));
        assertTrue(request.message().body().html().data().contains("Your verification code"));
        assertTrue(request.message().body().html().data().contains("Never share this code"));
    }
}
