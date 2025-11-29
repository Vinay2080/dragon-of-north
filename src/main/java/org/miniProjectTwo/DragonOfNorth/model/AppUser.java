package org.miniProjectTwo.DragonOfNorth.model;

import jakarta.persistence.*;
import lombok.*;
import org.miniProjectTwo.DragonOfNorth.common.BaseEntity;
import org.miniProjectTwo.DragonOfNorth.enums.UserStatus;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class AppUser extends BaseEntity {


    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Column(nullable = false)
    private boolean isEmailVerified = false;

    @Column(nullable = false)
    private boolean isPhoneNumberVerified = false;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private LocalDateTime lastLoginAt;


}
