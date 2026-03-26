package org.miniProjectTwo.DragonOfNorth.modules.otp.service;

/**
 * Abstraction for channel-specific OTP delivery (email, SMS, etc.).
 */

public interface OtpSender {

    /**
     * Sends an OTP to the target identifier.
     */
    void send(String identifier, String otp, int ttlMinutes);

}
