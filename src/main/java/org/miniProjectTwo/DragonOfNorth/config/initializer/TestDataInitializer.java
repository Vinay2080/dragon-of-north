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
import org.springframework.modulith.NamedInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.CREATED;
import static org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus.VERIFIED;
import static org.miniProjectTwo.DragonOfNorth.enums.RoleName.ADMIN;
import static org.miniProjectTwo.DragonOfNorth.enums.RoleName.USER;

/**
 * Initializes test data for development and testing environments.
 * Creates users with different authentication methods (email/phone), statuses,
 * and roles for testing purposes. Runs only in "test" profile with @Order(2)
 * after {@link RolesInitializer} ensures required roles exist.
 * Test users created with the default password "password123":
 * - 5 email users (user1-2@example.com, admin1-2@example.com, superadmin@example.com)
 * - 5 phone users (9912345601-9912345605)
 * - Various statuses: CREATED, VERIFIED
 * - Roles: USER, ADMIN, or both
 */
@NamedInterface
@Component
@Profile({"test"})
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after DataInitializer which has @Order(1)
public class TestDataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Entry point for test data initialization.
     * Retrieves USER and ADMIN roles from a database, then creates email and phone
     * users with different combinations of status and role assignments.
     *
     * @param args command line arguments (unused)
     * @throws IllegalStateException if required roles are not found
     */
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

    /**
     * Creates email-based test users with various statuses and roles.
     *
     * @param userRole  the USER role instance
     * @param adminRole the ADMIN role instance
     */
    private void initializeEmailUsers(Role userRole, Role adminRole) {
        // Create 5 email-only users with different statuses and roles
        createEmailUser("user1@example.com", CREATED, Set.of(userRole));
        createEmailUser("user2@example.com", VERIFIED, Set.of(userRole));
        createEmailUser("admin1@example.com", CREATED, Set.of(adminRole));
        createEmailUser("admin2@example.com", VERIFIED, Set.of(adminRole));
        createEmailUser("superadmin@example.com", VERIFIED, Set.of(userRole, adminRole));
    }

    /**
     * Creates phone-based test users with various statuses and roles.
     *
     * @param userRole the USER role instance
     * @param adminRole the ADMIN role instance
     */
    private void initializePhoneUsers(Role userRole, Role adminRole) {
        // Create 5 phone-only users with different statuses and roles
        createPhoneUser("9912345601", CREATED, Set.of(userRole));
        createPhoneUser("9912345602", VERIFIED, Set.of(userRole));
        createPhoneUser("9912345603", CREATED, Set.of(adminRole));
        createPhoneUser("9912345604", VERIFIED, Set.of(adminRole));
        createPhoneUser("9912345605", VERIFIED, Set.of(userRole, adminRole));
    }

    /**
     * Creates a new user with email authentication only.
     *
     * @param email the email address for the user
     * @param status the user status (CREATED or VERIFIED)
     * @param roles set of roles to assign to the user
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
     * Creates a new user with phone authentication only.
     *
     * @param phoneNumber the phone number for the user
     * @param status the user status (CREATED or VERIFIED)
     * @param roles set of roles to assign to the user
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
