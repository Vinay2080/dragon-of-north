package org.miniProjectTwo.DragonOfNorth.services.otp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PhoneOtpSenderTest {

    @InjectMocks
    private PhoneOtpSender phoneOtpSender;

    @Mock
    private SnsClient snsClient;

    @Test
    void send_shouldCallSnsClientWithCorrectRequest() {
        // arrange
        String phone = "1234567890";
        String otp = "123456";
        int ttlMinutes = 5;

        // act
        phoneOtpSender.send(phone, otp, ttlMinutes);

        // verify
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());

        PublishRequest request = captor.getValue();
        assertEquals(phone, request.phoneNumber());
        assertTrue(request.message().contains(otp));
        assertTrue(request.message().contains(String.valueOf(ttlMinutes)));
        assertEquals("Transactional", request.messageAttributes().get("AWS.SNS.SMS.SMSType").stringValue());
    }
}
