package org.miniProjectTwo.DragonOfNorth.impl.otp;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.services.OtpSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email OTP sender using AWS SES for delivery.
 * <p>
 * Asynchronously sends OTP codes via email through SES service. Delegates
 * email formatting and delivery to SesEmailService. Critical for email-based
 * authentication flows with reliable delivery tracking.
 *
 * @see SesEmailService for email formatting and AWS SES integration
 */
@Service
@RequiredArgsConstructor
public class EmailOtpSender implements OtpSender {
    private final SesEmailService sesEmailService;

    /**
     * Sends OTP code via email asynchronously.
     * <p>
     * Delegates to SesEmailService for email formatting and AWS SES delivery.
     * Runs asynchronously to prevent blocking main request threads.
     * Critical for timely OTP delivery in authentication flows.
     *
     * @param email recipient email address
     * @param otp generated OTP code
     * @param ttlMinutes OTP validity period
     */
    @Async
    @Override
    public void send(String email, String otp, int ttlMinutes) {
        sesEmailService.sendOtpEmail(email, otp, ttlMinutes);
    }


}
