package org.miniProjectTwo.DragonOfNorth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.miniProjectTwo.DragonOfNorth.common.BaseEntity;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a user in the system.
 * This entity extends {@link BaseEntity} to inherit common audit fields.
 * It is mapped to the 'users' table in the database.
 *
 * <p>This class includes user authentication and profile information
 * and uses JPA annotations for object-relational mapping.</p>
 *
 * @see BaseEntity
 * @see AppUserStatus
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
    private String phoneNumber;

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
     * Defaults to {@link AppUserStatus#ACTIVE} when a new user is created.
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


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "roles_id")
    )
    private Set<Role> roles = new HashSet<>();

}
// todo javadoc