package org.miniProjectTwo.DragonOfNorth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User entity with authentication and profile management.
 * <p>
 * Supports email/phone authentication with role-based access control.
 * Tracks login attempts, verification status, and account locking.
 * Critical for user lifecycle and security enforcement.
 *
 * @see Role for permission management
 * @see AppUserStatus for account states
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
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "phone_number")
        })


public class AppUser extends BaseEntity {

    /**
     * The unique phone number of the user.
     * This field is stored in the 'phone_number' column and must be unique across all users.
     * Can be used as an alternative to email for authentication.
     */
    @Column(name = "phone_number", unique = true)
    private String phone;

    /**
     * The unique email address of the user.
     * This field is used for authentication and communication.
     * Must be unique across all users in the system.
     */
    @Column(name = "email", unique = true)
    private String email;

    /**
     * The hashed password of the user.
     * This field is required and should always be stored in a hashed format.
     * Never store plain text passwords in the database.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * The current status of the user account.
     * Defaults to {@link AppUserStatus#CREATED} when a new user is created.
     * Can be used to block or delete user accounts while preserving their data.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppUserStatus appUserStatus;

    /**
     * Indicates whether the user's email address has been verified.
     * Defaults to false when a new user is created.
     * This flag is set to true after the user verifies their email address.
     */
    @Column(nullable = false)
    private boolean isEmailVerified = false;

    /**
     * Indicates whether the user's phone number has been verified.
     * Defaults to false when a new user is created.
     * This flag is set to true after the user verifies their phone number.
     */
    @Column(nullable = false)
    private boolean isPhoneNumberVerified = false;

    /**
     * The number of consecutive failed login attempts.
     * This counter is incremented on each failed login attempt and reset to zero
     * after a successful login. Can be used to implement account lockout after
     * a certain number of failed attempts.
     */
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(nullable = false)
    private boolean accountLocked = false;

    private LocalDateTime lockedAt;


    /**
     * The timestamp of the user's last successful login.
     * This field is automatically updated when the user successfully authenticates.
     * Can be used for security monitoring and session management.
     */
    private LocalDateTime lastLoginAt;


    /**
     * Checks if a user has any assigned roles.
     *
     * @return true if the roles collection is not null and not empty
     */
    public boolean hasAnyRoles() {
        return roles != null && !roles.isEmpty();
    }

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "roles_id")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Sessions> sessions = new ArrayList<>();

}