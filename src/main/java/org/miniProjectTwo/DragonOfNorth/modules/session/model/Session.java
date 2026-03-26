package org.miniProjectTwo.DragonOfNorth.modules.session.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.model.BaseEntity;

import java.time.Instant;

/**
 * Persistent refresh-token session bound to a user and device.
 */
@Getter
@Setter
@Entity
@Table(name = "user_sessions")
public class Session extends BaseEntity {

    /**
     * Owning a user account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id"
            , foreignKey = @ForeignKey(name = "fk_user_session_user"))
    private AppUser appUser;

    /**
     * Hash of the refresh token (raw token is never persisted).
     */
    @Column(name = "refresh_token_hash", nullable = false, length = 512)
    private String refreshTokenHash;

    /**
     * Client-provided device identifier.
     */
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    /**
     * Origin IP used for security/audit context.
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * User-agent string captured at login.
     */
    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    /**
     * Absolute expiration time for this session.
     */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /**
     * Last observed usage time.
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * Marks whether the session has been revoked.
     */
    @Column(nullable = false)
    private boolean revoked = false;

    /**
     * Returns whether the session is already past its expiry timestamp.
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(Instant.now());


    }
}