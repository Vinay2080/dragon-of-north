package org.miniProjectTwo.DragonOfNorth.services.otp;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.config.OtpConfig.SnsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * AWS SES email service for OTP delivery.
 * <p>
 * Formats and sends OTP emails via Amazon SES. Configures sender,
 * subject, and message body with OTP code and expiration.
 * Critical for reliable email delivery in authentication flows.
 *
 * @see SnsConfig for AWS region configuration
 */
@Service
@RequiredArgsConstructor
public class SesEmailService {

    private final SesClient sesClient;

    @Value("${aws.ses.sender}")
    private String sender;

    /**
     * Sends OTP email via AWS SES.
     * <p>
     * Formats email with OTP code, expiration time, and fixed subject.
     * Uses configured sender address from application properties.
     * Critical for email-based authentication delivery.
     *
     * @param to         recipient email address
     * @param otp        generated OTP code
     * @param ttlMinutes OTP validity period
     */
    public void sendOtpEmail(String to, String otp, int ttlMinutes) {

        SendEmailRequest request = SendEmailRequest
                .builder()
                .source(sender)
                .destination(Destination
                        .builder()
                        .toAddresses(to)
                        .build())
                .message(Message
                        .builder()
                        .subject(Content
                                .builder()
                                .data("Your OTP Code")
                                .build())
                        .body(Body
                                .builder()
                                .text(Content
                                        .builder()
                                        .data("Your OTP is " + otp + ". Expires in " + ttlMinutes + " minutes")
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }
}
