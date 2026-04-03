package org.miniProjectTwo.DragonOfNorth.modules.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.model.BaseEntity;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;

/**
 * Primary user aggregate for authentication, authorization, and profile ownership.
 *
 * <p>Supports email or phone identifiers, role assignments, verification flags, and lock state.</p>
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_phone", columnList = "phone_number")
        })


public class AppUser extends BaseEntity {

    /**
     * Optional phone identifier used for authentication.
     */
    @Column(name = "phone_number", unique = true)
    private String phone;

    /**
     * Optional email identifier used for authentication and communication.
     */
    @Column(name = "email", unique = true)
    private String email;

    /**
     * Password hash for local authentication.
     */
    @Column(name = "password")
    private String password;

    /**
     * Current lifecycle status of the account.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppUserStatus appUserStatus = ACTIVE;

    /**
     * Whether the email identifier has been verified.
     */
    @Column(nullable = false)
    private boolean isEmailVerified = false;

    /**
     * Whether the phone identifier has been verified.
     */
    @Column(nullable = false)
    private boolean isPhoneNumberVerified = false;

    /**
     * Consecutive failed login attempts used by lockout logic.
     */
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(nullable = false)
    private boolean accountLocked = false;

    /**
     * Timestamp when the account was locked.
     */
    private LocalDateTime lockedAt;


    /**
     * Timestamp of the most recent successful login.
     */
    private LocalDateTime lastLoginAt;

    /**
     * Timestamp when the account was soft deleted.
     */
    private LocalDateTime deletedAt;

    /**
     * Optional internal reason for account deletion.
     */
    @Column(name = "deletion_reason", length = 255)
    private String deletionReason;

    /**
     * Checks whether at least one role is assigned.
     *
     * @return {@code true} when roles are non-empty
     */
    public boolean hasAnyRoles() {
        return roles != null && !roles.isEmpty();
    }

    @ManyToMany(fetch = EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id",
                    foreignKey = @ForeignKey(name = "fk_user_roles_user")),
            inverseJoinColumns = @JoinColumn(name = "roles_id",
                    foreignKey = @ForeignKey(name = "fk_user_roles_role")
            )
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "appUser", cascade = ALL, fetch = LAZY)
    private List<Session> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = ALL, orphanRemoval = true, fetch = LAZY)
    private Set<UserAuthProvider> authProviders = new HashSet<>();

    @OneToOne(mappedBy = "appUser", cascade = ALL, orphanRemoval = true)
    private Profile profile;

}
