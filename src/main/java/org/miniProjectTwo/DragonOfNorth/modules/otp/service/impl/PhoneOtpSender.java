package org.miniProjectTwo.DragonOfNorth.modules.otp.service.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/**
 * Sends OTP messages over SMS via AWS SNS.
 */
@Service
@RequiredArgsConstructor
public class PhoneOtpSender implements OtpSender {
    private final SnsClient snsClient;

    /**
     * Sends a transactional OTP SMS asynchronously.
     */
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
    }
}
