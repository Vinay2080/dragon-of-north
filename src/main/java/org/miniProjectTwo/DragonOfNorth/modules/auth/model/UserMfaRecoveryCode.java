package org.miniProjectTwo.DragonOfNorth.modules.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.miniProjectTwo.DragonOfNorth.shared.model.BaseEntity;

import java.time.Instant;

/**
 * Hashed one-time recovery code for an MFA enrollment.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_mfa_recovery_codes")
public class UserMfaRecoveryCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "mfa_settings_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_mfa_recovery_codes_settings")
    )
    private UserMfaSettings mfaSettings;

    @Column(name = "recovery_code_hash", nullable = false)
    private String recoveryCodeHash;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "used_at")
    private Instant usedAt;

    public UserMfaRecoveryCode(UserMfaSettings mfaSettings, String recoveryCodeHash) {
        this.mfaSettings = mfaSettings;
        this.recoveryCodeHash = recoveryCodeHash;
    }

    public void markUsed(Instant usedAt) {
        this.used = true;
        this.usedAt = usedAt;
    }
}
