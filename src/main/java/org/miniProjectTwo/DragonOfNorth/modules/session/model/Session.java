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
     * Timestamp of when MFA was completed for this session.  Used to enforce "recent MFA" requirements
     * on sensitive operations.
     */
    @Column(name = "mfa_verified_at")
    private Instant mfaVerifiedAt;

    /**
     * Whether MFA is required for this session based on the user's MFA enrollment state at login time.
     */
    @Column(name = "mfa_required", nullable = false)
    private boolean mfaRequired;

    /**
     * Authentication method reference for the primary authentication (e.g. "password", "oauth2", etc.).
     */
    @Column(name = "primary_amr", nullable = false, length = 32)
    private String primaryAmr;

    /**
     * Authentication method reference for the MFA method used (e.g. "totp", "email_otp", etc.).
     */
    @Column(name = "mfa_method_amr", length = 32)
    private String mfaMethodAmr;

    /**
     * Returns whether the session is already past its expiry timestamp.
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(Instant.now());


    }
}
