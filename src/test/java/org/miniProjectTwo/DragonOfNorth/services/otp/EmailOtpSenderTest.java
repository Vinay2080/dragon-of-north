package org.miniProjectTwo.DragonOfNorth.services.otp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailOtpSenderTest {

    @InjectMocks
    private EmailOtpSender emailOtpSender;

    @Mock
    private SesEmailService sesEmailService;

    @Test
    void send_shouldCallSesEmailService() {
        // arrange
        String email = "test@example.com";
        String otp = "123456";
        int ttlMinutes = 5;

        // act
        emailOtpSender.send(email, otp, ttlMinutes);

        // verify
        verify(sesEmailService).sendOtpEmail(email, otp, ttlMinutes);
    }
}
