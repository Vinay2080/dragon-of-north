package org.miniProjectTwo.DragonOfNorth.impl.otp;

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

    @InjectMocks
    private SesEmailService sesEmailService;

    @Mock
    private SesClient sesClient;

    private final String sender = "noreply@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sesEmailService, "sender", sender);
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
        assertEquals(sender, request.source());
        assertEquals(to, request.destination().toAddresses().getFirst());
        assertTrue(request.message().body().text().data().contains(otp));
        assertTrue(request.message().body().text().data().contains(String.valueOf(ttlMinutes)));
    }
}
