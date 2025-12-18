package org.miniProjectTwo.DragonOfNorth.impl.otp;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@RequiredArgsConstructor
public class SesEmailService {

    private final SesClient sesClient;

    @Value("${aws.ses.sender}")
    private String sender;

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
