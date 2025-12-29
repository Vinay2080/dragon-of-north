package org.miniProjectTwo.DragonOfNorth.config;

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

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.*;
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
        createUserIfNotExists("created@example.com", "+1234567890", "password123", CREATED, null);
        createUserIfNotExists("verified@example.com", "+1234567891", "password123", VERIFIED, Set.of(userRole));
        createUserIfNotExists("active@example.com", "+1234567892", "password123", ACTIVE, Set.of(userRole));
        createUserIfNotExists("blocked@example.com", "+1234567893", "password123", BLOCKED, Set.of(userRole));
        createUserIfNotExists("admin@example.com", "+1234567894", "admin123", ACTIVE, Set.of(adminRole, userRole));

        log.info("Test users initialized successfully");
    }

    private void createUserIfNotExists(String email, String phoneNumber, String password, 
                                      AppUserStatus status, Set<Role> roles) {
        if (appUserRepository.findByEmail(email).isEmpty()) {
            AppUser user = new AppUser();
            user.setEmail(email);
            user.setPhone(phoneNumber);
            user.setPassword(passwordEncoder.encode(password));
            user.setAppUserStatus(status);
            user.setRoles(roles);
            
            // Set verification flags based on status
            if (status == VERIFIED || status == ACTIVE) {
                user.setEmailVerified(true);
                user.setPhoneNumberVerified(true);
            }
            
            if (status == ACTIVE) {
                user.setLastLoginAt(java.time.LocalDateTime.now());
            }
            
            appUserRepository.save(user);
            log.info("Created {} user with email: {}", status, email);
        }
    }
}
