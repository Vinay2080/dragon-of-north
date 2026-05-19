package org.miniProjectTwo.DragonOfNorth.modules.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.model.BaseEntity;

import java.time.Instant;

/**
 * Stores MFA factor material separately from the core user aggregate.
 *
 * <p>Only encrypted secrets belong here; plaintext TOTP secrets must remain
 * limited to the short-lived setup/verification path.</p>
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_mfa_settings")
public class UserMfaSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_user_mfa_settings_user")
    )
    private AppUser user;

    @Column(name = "totp_secret_encrypted", nullable = false, columnDefinition = "text")
    private String totpSecretEncrypted;

    @Column(name = "totp_enabled_at", nullable = false)
    private Instant totpEnabledAt;

    public UserMfaSettings(AppUser user, String totpSecretEncrypted, Instant totpEnabledAt) {
        this.user = user;
        this.totpSecretEncrypted = totpSecretEncrypted;
        this.totpEnabledAt = totpEnabledAt;
    }
}
