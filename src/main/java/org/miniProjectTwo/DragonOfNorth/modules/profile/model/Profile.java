package org.miniProjectTwo.DragonOfNorth.modules.profile.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.model.BaseEntity;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "user_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_profiles_user_id", columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Profile extends BaseEntity {

    @OneToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "avatar_source", length = 32, nullable = false)
    private AvatarSource avatarSource = AvatarSource.NONE;

    @Column(name = "avatar_external_url", length = 500)
    private String avatarExternalUrl;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "username", length = 50, unique = true)
    private String username;

    @PrePersist
    @PreUpdate
    private void normalizeAvatarMetadata() {
        boolean hasAvatar = avatarUrl != null && !avatarUrl.isBlank();

        if (avatarSource == null) {
            avatarSource = hasAvatar ? AvatarSource.USER_DEFINED : AvatarSource.NONE;
        }

        if (avatarSource == AvatarSource.NONE && !hasAvatar) {
            avatarExternalUrl = null;
        }
    }
}
