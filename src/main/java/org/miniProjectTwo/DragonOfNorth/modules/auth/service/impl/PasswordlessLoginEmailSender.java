package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.modules.otp.service.impl.SesEmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends passwordless login links over email.
 */
@Service
@RequiredArgsConstructor
public class PasswordlessLoginEmailSender {

    private final SesEmailService sesEmailService;

    /**
     * Asynchronously sends a passwordless login email.
     *
     * @param email      the recipient's email address
     * @param loginLink  the passwordless login link to include in the email
     * @param ttlMinutes the time-to-live for the login link, in minutes
     */
    @Async
    public void send(String email, String loginLink, long ttlMinutes) {
        sesEmailService.sendPasswordlessLoginEmail(email, loginLink, ttlMinutes);
    }
}
