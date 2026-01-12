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

        // Initialize email users
        initializeEmailUsers(userRole, adminRole);
        
        // Initialize phone users
        initializePhoneUsers(userRole, adminRole);
        
        log.info("Test users initialized successfully");
    }
    
    private void initializeEmailUsers(Role userRole, Role adminRole) {
        // Create 5 email-only users with different statuses and roles
        createEmailUser("user1@example.com", CREATED, Set.of(userRole));
        createEmailUser("user2@example.com", VERIFIED, Set.of(userRole));
        createEmailUser("admin1@example.com", CREATED, Set.of(adminRole));
        createEmailUser("admin2@example.com", VERIFIED, Set.of(adminRole));
        createEmailUser("superadmin@example.com", VERIFIED, Set.of(userRole, adminRole));
    }
    
    private void initializePhoneUsers(Role userRole, Role adminRole) {
        // Create 5 phone-only users with different statuses and roles
        createPhoneUser("9912345601", CREATED, Set.of(userRole));
        createPhoneUser("9912345602", VERIFIED, Set.of(userRole));
        createPhoneUser("9912345603", CREATED, Set.of(adminRole));
        createPhoneUser("9912345604", VERIFIED, Set.of(adminRole));
        createPhoneUser("9912345605", VERIFIED, Set.of(userRole, adminRole));
    }

    /**
     * Creates a new user with email authentication only
     */
    private void createEmailUser(String email, AppUserStatus status, Set<Role> roles) {
        if (appUserRepository.findByEmail(email).isPresent()) {
            return;
        }
        
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAppUserStatus(status);
        user.setRoles(roles);
        
        if (status == VERIFIED) {
            user.setEmailVerified(true);
        }
        
        appUserRepository.save(user);
        log.info("Created {} email user: {}", status, email);
    }
    
    /**
     * Creates a new user with phone authentication only
     */
    private void createPhoneUser(String phoneNumber, AppUserStatus status, Set<Role> roles) {
        if (appUserRepository.findByPhone(phoneNumber).isPresent()) {
            return;
        }
        
        AppUser user = new AppUser();
        user.setPhone(phoneNumber);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAppUserStatus(status);
        user.setRoles(roles);
        
        if (status == VERIFIED) {
            user.setPhoneNumberVerified(true);
        }
        
        appUserRepository.save(user);
        log.info("Created {} phone user: {}", status, phoneNumber);
    }

}
