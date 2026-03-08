package org.miniProjectTwo.DragonOfNorth.infrastructure.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.Provider.LOCAL;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName.ADMIN;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName.USER;

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
    private final SessionRepository sessionRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
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

        // Initialize deterministic sessions for testing session endpoints
        initializeTestSessions();
        
        log.info("Test users initialized successfully");
    }

    private void initializeTestSessions() {
        appUserRepository.findByEmail("user2@example.com")
                .ifPresent(user -> {
                    createSessionIfAbsent(user, "test-device-web", "127.0.0.10", "Chrome/Test");
                    createSessionIfAbsent(user, "test-device-mobile", "127.0.0.11", "Mobile Safari/Test");
                });

        appUserRepository.findByEmail("admin2@example.com")
                .ifPresent(user -> createSessionIfAbsent(user, "test-device-admin", "127.0.0.12", "Firefox/Test"));
    }

    private void createSessionIfAbsent(AppUser user, String deviceId, String ipAddress, String userAgent) {
        if (sessionRepository.findByAppUserAndDeviceId(user, deviceId).isPresent()) {
            return;
        }

        Session session = new Session();
        session.setAppUser(user);
        session.setDeviceId(deviceId);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setRefreshTokenHash("seed-refresh-token-hash-" + deviceId);
        session.setLastUsedAt(Instant.now());
        session.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));
        session.setRevoked(false);

        sessionRepository.save(session);
        log.info("Created test session for user {} and device {}", user.getId(), deviceId);
    }

    /**
     * Creates email-based test users with various statuses and roles.
     *
     * @param userRole  the USER role instance
     * @param adminRole the ADMIN role instance
     */
    private void initializeEmailUsers(Role userRole, Role adminRole) {
        // Create 5 email-only users with different statuses and roles
        createEmailUser("user1@example.com", false, Set.of(userRole));
        createEmailUser("user2@example.com", true, Set.of(userRole));
        createEmailUser("admin1@example.com", false, Set.of(adminRole));
        createEmailUser("admin2@example.com", true, Set.of(adminRole));
        createEmailUser("superadmin@example.com", true, Set.of(userRole, adminRole));
    }

    /**
     * Creates phone-based test users with various statuses and roles.
     *
     * @param userRole the USER role instance
     * @param adminRole the ADMIN role instance
     */
    private void initializePhoneUsers(Role userRole, Role adminRole) {
        // Create 5 phone-only users with different statuses and roles
        createPhoneUser("9912345601", false, Set.of(userRole));
        createPhoneUser("9912345602", true, Set.of(userRole));
        createPhoneUser("9912345603", false, Set.of(adminRole));
        createPhoneUser("9912345604", true, Set.of(adminRole));
        createPhoneUser("9912345605", true, Set.of(userRole, adminRole));
    }

    /**
     * Creates a new user with email authentication only.
     *
     * @param email the email address for the user
     * @param roles set of roles to assign to the user
     */
    private void createEmailUser(String email, boolean emailVerified, Set<Role> roles) {
        if (appUserRepository.findByEmail(email).isPresent()) {
            return;
        }
        
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAppUserStatus(ACTIVE);
        user.setRoles(roles);

        user.setEmailVerified(emailVerified);

        AppUser savedUser = appUserRepository.save(user);
        createLocalProvider(savedUser);
        log.info("Created ACTIVE email user: {}", email);
    }

    /**
     * Creates a new user with phone authentication only.
     *
     * @param phoneNumber the phone number for the user
     * @param roles set of roles to assign to the user
     */
    private void createPhoneUser(String phoneNumber, boolean phoneVerified, Set<Role> roles) {
        if (appUserRepository.findByPhone(phoneNumber).isPresent()) {
            return;
        }
        
        AppUser user = new AppUser();
        user.setPhone(phoneNumber);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAppUserStatus(ACTIVE);
        user.setRoles(roles);
        user.setPhoneNumberVerified(phoneVerified);

        AppUser savedUser = appUserRepository.save(user);
        createLocalProvider(savedUser);
        log.info("Created ACTIVE phone user: {}", phoneNumber);
    }

    private void createLocalProvider(AppUser appUser) {
        if (userAuthProviderRepository.existsByUserIdAndProvider(appUser.getId(), LOCAL)) {
            return;
        }
        UserAuthProvider provider = new UserAuthProvider();
        provider.setUser(appUser);
        provider.setProvider(LOCAL);
        userAuthProviderRepository.save(provider);
    }

}
