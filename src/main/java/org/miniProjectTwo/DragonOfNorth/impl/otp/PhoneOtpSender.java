package org.miniProjectTwo.DragonOfNorth.impl.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.services.OtpSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor

public class PhoneOtpSender implements OtpSender {
    private final SnsClient snsClient;

    @Async
    @Override
    public void send(String phone, String otp, int ttlMinutes) {
        String message = "Your OTP is " + otp + ". Expires in " + ttlMinutes + " minutes.";

        PublishRequest request = PublishRequest
                .builder()
                .phoneNumber(phone)
                .message(message)
                .messageAttributes(Map.of(
                        "AWS.SNS.SMS.SMSType",
                        MessageAttributeValue
                                .builder()
                                .dataType("String")
                                .stringValue("Transactional")
                                .build()
                ))
                .build();
        snsClient.publish(request);
        log.info("SMS OTP sent to {}", phone);
    }
}
