package org.miniProjectTwo.DragonOfNorth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "user_sessions")
public class Session extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id"
            , foreignKey = @ForeignKey(name = "fk_user_session_user"))
    private AppUser appUser;

    @Column(name = "refresh_token_hash", nullable = false, length = 512)
    private String refreshTokenHash;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(nullable = false)
    private boolean revoked = false;

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(Instant.now());


    }
}