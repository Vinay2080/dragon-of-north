package org.miniProjectTwo.DragonOfNorth.impl.otp;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.services.OtpSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailOtpSender implements OtpSender {
    private final SesEmailService sesEmailService;
    @Override
    public void send(String email, String otp, int ttlMinutes) {
        sesEmailService.sendOtpEmail(email, otp, ttlMinutes);
    }


}
