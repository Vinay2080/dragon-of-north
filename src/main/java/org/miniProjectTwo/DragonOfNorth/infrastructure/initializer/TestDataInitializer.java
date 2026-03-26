package org.miniProjectTwo.DragonOfNorth.infrastructure.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserAuthProvider;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.profile.repo.ProfileRepository;
import org.miniProjectTwo.DragonOfNorth.modules.profile.service.ProfileService;
import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;
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
 * Seeds deterministic users and sessions for local/test environments.
 *
 * <p>Runs after {@link RolesInitializer} so role records are already available.</p>
 */
@NamedInterface
@Component
@Profile({"test", "dev"})
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after DataInitializer which has @Order(1)
public class TestDataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final SessionRepository sessionRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final ProfileRepository profileRepository;
    private int createdEmailUsers;
    private int createdPhoneUsers;
    private int createdSessions;
    private int createdProfiles;

    /**
     * Loads seed users and seed sessions.
     *
     * @param args command-line arguments (unused)
     * @throws IllegalStateException when required roles are missing
     */
    @Override
    public void run(String @NonNull ... args) {
        createdEmailUsers = 0;
        createdPhoneUsers = 0;
        createdSessions = 0;
        createdProfiles = 0;

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

        log.info("Test data initialization completed. emailUsersCreated={}, phoneUsersCreated={}, sessionsCreated={}, profilesCreated={}",
                createdEmailUsers, createdPhoneUsers, createdSessions, createdProfiles);
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

    /**
     * Creates seed users that authenticate with email.
     *
     * @param userRole USER role
     * @param adminRole ADMIN role
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
     * Creates seed users that authenticate with phone numbers.
     *
     * @param userRole USER role
     * @param adminRole ADMIN role
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
     * Creates an email user when the identifier is not already present.
     *
     * @param email email identifier
     * @param roles roles to assign
     */
    private void createEmailUser(String email, boolean emailVerified, Set<Role> roles) {
        OAuthUserInfo userInfo = buildSeedUserInfo(email, email);
        AppUser existingUser = appUserRepository.findByEmail(email).orElse(null);
        if (existingUser != null) {
            ensureProfileIfAbsent(existingUser, userInfo);
            log.debug("Skipped seed email user creation because identifier already exists");
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
        ensureProfileIfAbsent(savedUser, userInfo);
        createdEmailUsers++;
        log.debug("Created seed email user with verified={}", emailVerified);
    }

    /**
     * Creates a phone user when the identifier is not already present.
     *
     * @param phoneNumber phone identifier
     * @param roles roles to assign
     */
    private void createPhoneUser(String phoneNumber, boolean phoneVerified, Set<Role> roles) {
        OAuthUserInfo userInfo = buildSeedUserInfo(phoneNumber, null);
        AppUser existingUser = appUserRepository.findByPhone(phoneNumber).orElse(null);
        if (existingUser != null) {
            ensureProfileIfAbsent(existingUser, userInfo);
            log.debug("Skipped seed phone user creation because identifier already exists");
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
        ensureProfileIfAbsent(savedUser, userInfo);
        createdPhoneUsers++;
        log.debug("Created seed phone user with verified={}", phoneVerified);
    }

    private OAuthUserInfo buildSeedUserInfo(String displayName, String email) {
        return OAuthUserInfo.builder()
                .sub(null)
                .email(email)
                .emailVerified(email != null)
                .name(displayName)
                .picture(null)
                .issuer("seed-data")
                .audience("seed-data")
                .expirationTime(null)
                .issuedAtTime(null)
                .build();
    }

    private void ensureProfileIfAbsent(AppUser appUser, OAuthUserInfo userInfo) {
        if (profileRepository.existsProfileByAppUser(appUser)) {
            log.debug("Skipped profile creation because profile already exists for userId={}", appUser.getId());
            return;
        }
        profileService.createProfile(appUser, userInfo);
        createdProfiles++;
        log.debug("Created seed profile for userId={}", appUser.getId());
    }

    private void createSessionIfAbsent(AppUser user, String deviceId, String ipAddress, String userAgent) {
        if (sessionRepository.findByAppUserAndDeviceId(user, deviceId).isPresent()) {
            log.debug("Skipped seed session creation for existing deviceId={}", deviceId);
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
        createdSessions++;
        log.debug("Created seed session for deviceId={}", deviceId);
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
