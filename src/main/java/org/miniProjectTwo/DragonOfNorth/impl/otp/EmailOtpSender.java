package org.miniProjectTwo.DragonOfNorth.impl.otp;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.services.OtpSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link OtpSender} that sends One-Time Passwords (OTP) via email
 * using Amazon Simple Email Service (SES).
 * This service is responsible for delivering OTPs to users' email addresses.
 *
 * @see OtpSender
 * @see SesEmailService
 */

@Service
@RequiredArgsConstructor
public class EmailOtpSender implements OtpSender {
    private final SesEmailService sesEmailService;

    /**
     * Sends an OTP to the specified email address.
     * The OTP is delivered asynchronously through AWS SES.
     *
     * @param email The recipient's email address
     * @param otp The one-time password to be sent
     * @param ttlMinutes Time-to-live for the OTP in minutes
     * @throws IllegalArgumentException if the email address is invalid
     */

    @Async
    @Override
    public void send(String email, String otp, int ttlMinutes) {
        sesEmailService.sendOtpEmail(email, otp, ttlMinutes);
    }


}
