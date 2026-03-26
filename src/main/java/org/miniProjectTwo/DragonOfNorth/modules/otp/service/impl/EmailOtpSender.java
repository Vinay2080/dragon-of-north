package org.miniProjectTwo.DragonOfNorth.modules.otp.service.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.OtpSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends OTP messages over email by delegating to {@link SesEmailService}.
 */
@Service
@RequiredArgsConstructor
public class EmailOtpSender implements OtpSender {
    private final SesEmailService sesEmailService;

    /**
     * Sends an OTP email asynchronously.
     */
    @Async
    @Override
    public void send(String email, String otp, int ttlMinutes) {
        sesEmailService.sendOtpEmail(email, otp, ttlMinutes);
    }


}
