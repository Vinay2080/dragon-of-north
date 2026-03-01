package org.miniProjectTwo.DragonOfNorth.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppUserTest {

    private AppUser appUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        appUser = new AppUser();
        testRole = new Role();
        testRole.setRoleName(org.miniProjectTwo.DragonOfNorth.enums.RoleName.USER);
    }

    @Test
    void constructor_shouldCreateEmptyUser() {
        // assert
        assertNotNull(appUser);
        assertNull(appUser.getId());
        assertNull(appUser.getEmail());
        assertNull(appUser.getPhone());
        assertNull(appUser.getPassword());
        assertEquals(AppUserStatus.ACTIVE, appUser.getAppUserStatus());
        assertFalse(appUser.isEmailVerified());
        assertFalse(appUser.isPhoneNumberVerified());
        assertEquals(0, appUser.getFailedLoginAttempts());
        assertFalse(appUser.isAccountLocked());
        assertNull(appUser.getLockedAt());
        assertNull(appUser.getLastLoginAt());
        assertNotNull(appUser.getRoles());
        assertTrue(appUser.getRoles().isEmpty());
        assertNotNull(appUser.getSessions());
        assertTrue(appUser.getSessions().isEmpty());
    }

    @Test
    void constructorWithAllArgs_shouldCreateUser() {
        // arrange
        UUID id = UUID.randomUUID();
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);

        // act
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("test@example.com");
        user.setPhone("+1234567890");
        user.setPassword("hashedPassword");
        user.setAppUserStatus(AppUserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPhoneNumberVerified(true);
        user.setFailedLoginAttempts(2);
        user.setAccountLocked(true);
        user.setLockedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        user.setRoles(roles);

        // assert
        assertEquals(id, user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("+1234567890", user.getPhone());
        assertEquals("hashedPassword", user.getPassword());
        assertEquals(AppUserStatus.ACTIVE, user.getAppUserStatus());
        assertTrue(user.isEmailVerified());
        assertTrue(user.isPhoneNumberVerified());
        assertEquals(2, user.getFailedLoginAttempts());
        assertTrue(user.isAccountLocked());
        assertNotNull(user.getLockedAt());
        assertNotNull(user.getLastLoginAt());
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().contains(testRole));
    }

    @Test
    void hasAnyRoles_shouldReturnFalse_whenRolesIsNull() {
        // arrange
        appUser.setRoles(null);

        // act & assert
        assertFalse(appUser.hasAnyRoles());
    }

    @Test
    void hasAnyRoles_shouldReturnFalse_whenRolesIsEmpty() {
        // arrange
        appUser.setRoles(new HashSet<>());

        // act & assert
        assertFalse(appUser.hasAnyRoles());
    }

    @Test
    void hasAnyRoles_shouldReturnTrue_whenRolesHasElements() {
        // arrange
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);
        appUser.setRoles(roles);

        // act & assert
        assertTrue(appUser.hasAnyRoles());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // arrange
        LocalDateTime now = LocalDateTime.now();
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);

        // act
        appUser.setEmail("new@example.com");
        appUser.setPhone("+9876543210");
        appUser.setPassword("newPassword");
        appUser.setAppUserStatus(AppUserStatus.LOCKED);
        appUser.setEmailVerified(true);
        appUser.setPhoneNumberVerified(true);
        appUser.setFailedLoginAttempts(5);
        appUser.setAccountLocked(true);
        appUser.setLockedAt(now);
        appUser.setLastLoginAt(now);
        appUser.setRoles(roles);

        // assert
        assertEquals("new@example.com", appUser.getEmail());
        assertEquals("+9876543210", appUser.getPhone());
        assertEquals("newPassword", appUser.getPassword());
        assertEquals(AppUserStatus.LOCKED, appUser.getAppUserStatus());
        assertTrue(appUser.isEmailVerified());
        assertTrue(appUser.isPhoneNumberVerified());
        assertEquals(5, appUser.getFailedLoginAttempts());
        assertTrue(appUser.isAccountLocked());
        assertEquals(now, appUser.getLockedAt());
        assertEquals(now, appUser.getLastLoginAt());
        assertEquals(roles, appUser.getRoles());
    }

    @Test
    void inheritance_shouldExtendBaseEntity() {
        // assert
        assertInstanceOf(BaseEntity.class, appUser);
    }
}
