package org.miniProjectTwo.DragonOfNorth.modules.otp.service.impl;

import lombok.RequiredArgsConstructor;
import org.miniProjectTwo.DragonOfNorth.infrastructure.otpconfig.SesConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * Composes and sends OTP emails using AWS SES.
 *
 * @see SesConfig for SES client configuration
 */
@Service
@RequiredArgsConstructor
public class SesEmailService {

    private static final String DISPLAY_NAME = "Dragon of North";
    private static final String SUBJECT = "Your verification code";

    private final SesClient sesClient;

    @Value("${aws.ses.sender}")
    private String sender;

    /**
     * Sends an OTP email to the recipient.
     */
    public void sendOtpEmail(String to, String otp, int ttlMinutes) {
        String htmlBody = buildHtmlBody(otp, ttlMinutes);
        String textBody = buildPlainTextBody(otp, ttlMinutes);

        SendEmailRequest request = SendEmailRequest
                .builder()
                .source(buildSourceAddress())
                .destination(Destination
                        .builder()
                        .toAddresses(to)
                        .build())
                .message(Message
                        .builder()
                        .subject(Content
                                .builder()
                                .charset("UTF-8")
                                .data(SUBJECT)
                                .build())
                        .body(Body
                                .builder()
                                .text(Content
                                        .builder()
                                        .charset("UTF-8")
                                        .data(textBody)
                                        .build())
                                .html(Content
                                        .builder()
                                        .charset("UTF-8")
                                        .data(htmlBody)
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }

    private String buildHtmlBody(String otp, int ttlMinutes) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <body style="margin:0;padding:0;background-color:#f3f5f7;font-family:Arial,Helvetica,sans-serif;">
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f3f5f7;padding:24px 0;">
                    <tr>
                      <td align="center" style="padding:0 12px;">
                        <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0" style="width:100%;max-width:600px;background-color:#ffffff;border:1px solid #e5e7eb;border-radius:8px;">
                          <tr>
                            <td style="padding:24px 24px 12px 24px;background-color:#1f3b73;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:0.4px;">
                              Dragon of North
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:24px;">
                              <h1 style="margin:0 0 16px 0;font-size:24px;line-height:1.3;color:#111827;">Your verification code</h1>
                              <p style="margin:0 0 16px 0;font-size:16px;line-height:1.5;color:#374151;">Use the code below to continue your sign-in.</p>
                              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 16px 0;">
                                <tr>
                                  <td align="center" style="padding:18px;background-color:#f9fafb;border:1px solid #d1d5db;border-radius:8px;">
                                    <span style="display:inline-block;font-size:34px;line-height:1;font-weight:700;letter-spacing:8px;color:#111827;">{{OTP_DISPLAY}}</span>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:0 0 10px 0;font-size:15px;line-height:1.5;color:#374151;">This code expires in <strong>{{TTL_MINUTES}} minutes</strong>.</p>
                              <p style="margin:0 0 14px 0;font-size:15px;line-height:1.5;color:#b91c1c;"><strong>Security notice:</strong> Never share this code with anyone.</p>
                              <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 12px 0;">
                                <tr>
                                  <td style="background-color:#1f3b73;border-radius:6px;padding:10px 16px;color:#ffffff;font-size:14px;font-weight:600;">
                                    Copy code: {{OTP_RAW}}
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:0;font-size:13px;line-height:1.5;color:#6b7280;">If you did not request this code, you can safely ignore this email.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:16px 24px;background-color:#f9fafb;border-top:1px solid #e5e7eb;font-size:12px;line-height:1.5;color:#6b7280;">
                              Dragon of North Authentication - This is an automated message.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """
                .replace("{{OTP_DISPLAY}}", formatOtpForDisplay(otp))
                .replace("{{TTL_MINUTES}}", String.valueOf(ttlMinutes))
                .replace("{{OTP_RAW}}", otp);
    }

    private String buildPlainTextBody(String otp, int ttlMinutes) {
        return "Dragon of North\n"
                + "Your verification code\n\n"
                + "Code: " + formatOtpForDisplay(otp) + "\n"
                + "This code expires in " + ttlMinutes + " minutes.\n\n"
                + "Never share this code with anyone.\n\n"
                + "If you did not request this code, you can safely ignore this email.";
    }

    private String buildSourceAddress() {
        if (sender.contains("<") && sender.contains(">")) {
            return sender;
        }
        return DISPLAY_NAME + " <" + sender + ">";
    }

    private String formatOtpForDisplay(String otp) {
        return otp.replaceAll("(.{3})(?!$)", "$1 ");
    }
}
