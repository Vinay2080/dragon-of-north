package org.miniProjectTwo.DragonOfNorth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JWT refresh token entity for session management.
 * <p>
 * Stores hashed refresh tokens with expiration and revocation tracking.
 * Links to user for token-based authentication renewal.
 * Critical for maintaining secure user sessions.
 *
 * @see AppUser for token ownership
 */
@Getter
@Setter
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_prefix", columnList = "token_prefix")
})
public class RefreshToken extends BaseEntity {

    @Column(name = "token_hash", nullable = false, unique = true)
    private String token;

    @Column(name = "token_prefix", length = 8)
    private String tokenPrefix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column
    private Instant lastUsed;

    /**
     * Checks if the refresh token has expired.
     *
     * @return true if the expiration date is before the current time
     */
    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }

}
