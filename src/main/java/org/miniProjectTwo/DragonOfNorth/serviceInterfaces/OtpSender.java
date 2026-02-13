package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

/**
 * Service interface for sending One-Time Passwords (OTP) to users through various channels.
 * Implementations of this interface handle the delivery mechanism for OTPs, which could be
 * through email, SMS, or other communication channels.
 */

public interface OtpSender {

    /**
     * Sends an OTP to the specified recipient.
     *
     * @param identifier The recipient's unique identifier (e.g., email address or phone number)
     * @param otp The one-time password to be sent
     * @param ttlMinutes Time-to-live for the OTP in minutes
     * @throws IllegalArgumentException if the identifier is invalid
     */
    void send(String identifier, String otp, int ttlMinutes);

}
