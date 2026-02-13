package org.miniProjectTwo.DragonOfNorth.services.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.config.OtpConfig.SnsConfig;
import org.miniProjectTwo.DragonOfNorth.serviceInterfaces.OtpSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

/**
 * SMS OTP sender using AWS SNS for delivery.
 * <p>
 * Asynchronously sends OTP codes via SMS through SNS service.
 * Formats a message with OTP and expiration, uses a Transactional SMS type.
 * Critical for phone-based authentication flows with reliable delivery.
 *
 * @see SnsConfig for SNS client configuration
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PhoneOtpSender implements OtpSender {
    private final SnsClient snsClient;

    /**
     * Sends OTP code via SMS asynchronously.
     * <p>
     * Formats a message with OTP and expiration, sets a Transactional SMS type,
     * and publishes via AWS SNS. Runs asynchronously to prevent blocking.
     * Critical for timely OTP delivery in phone authentication.
     *
     * @param phone      recipient phone number
     * @param otp        generated OTP code
     * @param ttlMinutes OTP validity period
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
        log.info("SMS OTP sent to {}", phone);
    }
}
