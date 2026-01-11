package org.miniProjectTwo.DragonOfNorth.config.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.VERIFIED;
import static org.miniProjectTwo.DragonOfNorth.enums.RoleName.ADMIN;
import static org.miniProjectTwo.DragonOfNorth.enums.RoleName.USER;

/**
 * Initializes test data for development and testing environments.
 * Creates users with different statuses for testing purposes.
 */
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after DataInitializer which has @Order(1)
public class TestDataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String @NonNull ... args) {
        // Get existing roles (they should be created by DataInitializer)
        Role userRole = roleRepository.findByRoleName(USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found. Make sure DataInitializer has run first."));

        Role adminRole = roleRepository.findByRoleName(ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found. Make sure DataInitializer has run first."));

        // Create test users with different statuses
        createUserIfNotExists("created@example.com", "+1234567890", CREATED, null);
        createUserIfNotExists("verified@example.com", "+1234567891", VERIFIED, Set.of(userRole));
        createUserIfNotExists("admin@gmail.com", "7897897890", VERIFIED, Set.of(adminRole));
        log.info("Test users initialized successfully");
    }

    private void createUserIfNotExists(String email, String phoneNumber,
                                       AppUserStatus status, Set<Role> roles) {
        if (appUserRepository.findByEmail(email).isEmpty()) {
            AppUser user = new AppUser();
            user.setEmail(email);
            user.setPhone(phoneNumber);
            user.setPassword(passwordEncoder.encode("password123"));
            user.setAppUserStatus(status);
            user.setRoles(roles);

            // Set verification flags based on status
            if (status == VERIFIED) {
                user.setEmailVerified(true);
                user.setPhoneNumberVerified(true);
            }

            appUserRepository.save(user);
            log.info("Created {} user with email: {}", status, email);
        }
    }
}
