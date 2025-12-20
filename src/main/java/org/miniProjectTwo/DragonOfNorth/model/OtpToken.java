package org.miniProjectTwo.DragonOfNorth.model;

import jakarta.persistence.*;
import lombok.*;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Entity
@Table(
        name = "otp_tokens",
        indexes = {
                @Index(name = "idx_otp_identifier_type_created",
                        columnList = "identifier,type,created_at"),
                @Index(name = "idx_otp_expires_at",
                        columnList = "expires_at"),
                @Index(name = "idx_otp_identifier_type_last_sent",
                        columnList = "identifier,type,last_sent_at")
                // todo understand indexing.
        }
)
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false, length = 256)
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private IdentifierType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "otp_purpose")
    private OtpPurpose otpPurpose;

    @Column(nullable = false, length = 200)
    private String otpHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean consumed;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Version
    private Long version;

    public OtpToken(String identifier, IdentifierType type, String otpHash, int ttlMinutes, OtpPurpose otpPurpose) {
        this.identifier = identifier;
        this.type = type;
        this.otpHash = otpHash;
        this.createdAt = Instant.now();
        this.lastSentAt = this.createdAt;
        this.expiresAt = this.createdAt.plusSeconds(ttlMinutes * 60L);
        this.otpPurpose = otpPurpose;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void incrementAttempts() {
        this.attempts += 1;
    }

    public void markVerified() {
        this.consumed = true;
        this.verifiedAt = Instant.now();
    }

}
