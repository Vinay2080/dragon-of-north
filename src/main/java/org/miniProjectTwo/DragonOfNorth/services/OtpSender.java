package org.miniProjectTwo.DragonOfNorth.services;

public interface OtpSender {

    void send(String identifier, String otp, int ttlMinutes);

}
//package org.miniProjectTwo.auth.otp;
//
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.Instant;
//
//@Component
//public class OtpCleanupTask {
//    private final EmailOtpRepository repo;
//    public OtpCleanupTask(EmailOtpRepository repo) { this.repo = repo; }
//
//    // runs every hour
//    @Scheduled(fixedDelayString = "${otp.cleanup.delay-ms:3600000}")
//    public void cleanupExpired() {
//        repo.deleteAllByExpiresAtBefore(Instant.now());
//    }
//}
