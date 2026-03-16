package org.miniProjectTwo.DragonOfNorth.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.model.BaseEntity;
import org.miniProjectTwo.DragonOfNorth.shared.model.Permission;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    private Role role;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
        role = new Role();
        testPermission = new Permission();
        testPermission.setName("READ_PERMISSION");
    }

    @Test
    void constructor_shouldCreateEmptyRole() {
        // assert
        assertNotNull(role);
        assertNull(role.getRoleName());
        assertTrue(role.isSystemRole()); // defaults to true
        assertNotNull(role.getPermissions());
        assertTrue(role.getPermissions().isEmpty());
        assertNotNull(role.getAppUsers());
        assertTrue(role.getAppUsers().isEmpty());
    }

    @Test
    void constructorWithAllArgs_shouldCreateRole() {
        // arrange
        UUID id = UUID.randomUUID();
        Set<Permission> permissions = new HashSet<>();
        permissions.add(testPermission);
        Set<AppUser> appUsers = new HashSet<>();
        appUsers.add(new AppUser());

        // act
        Role newRole = new Role();
        newRole.setId(id);
        newRole.setRoleName(RoleName.ADMIN);
        newRole.setSystemRole(false);
        newRole.setPermissions(permissions);
        newRole.setAppUsers(appUsers);

        // assert
        assertEquals(id, newRole.getId());
        assertEquals(RoleName.ADMIN, newRole.getRoleName());
        assertFalse(newRole.isSystemRole());
        assertEquals(permissions, newRole.getPermissions());
        assertEquals(appUsers, newRole.getAppUsers());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // arrange
        Set<Permission> permissions = new HashSet<>();
        permissions.add(testPermission);
        Set<AppUser> appUsers = new HashSet<>();
        appUsers.add(new AppUser());

        // act
        role.setRoleName(RoleName.USER);
        role.setSystemRole(false);
        role.setPermissions(permissions);
        role.setAppUsers(appUsers);

        // assert
        assertEquals(RoleName.USER, role.getRoleName());
        assertFalse(role.isSystemRole());
        assertEquals(permissions, role.getPermissions());
        assertEquals(appUsers, role.getAppUsers());
    }

    @Test
    void systemRole_shouldDefaultToTrue() {
        // assert
        assertTrue(role.isSystemRole());
    }

    @Test
    void systemRole_shouldAcceptFalse() {
        // act
        role.setSystemRole(false);

        // assert
        assertFalse(role.isSystemRole());
    }

    @Test
    void systemRole_shouldAcceptTrue() {
        // act
        role.setSystemRole(true);

        // assert
        assertTrue(role.isSystemRole());
    }

    @Test
    void permissions_shouldAcceptEmptySet() {
        // act
        role.setPermissions(new HashSet<>());

        // assert
        assertNotNull(role.getPermissions());
        assertTrue(role.getPermissions().isEmpty());
    }

    @Test
    void permissions_shouldAcceptNull() {
        // act
        role.setPermissions(null);

        // assert
        assertNull(role.getPermissions());
    }

    @Test
    void appUsers_shouldAcceptEmptySet() {
        // act
        role.setAppUsers(new HashSet<>());

        // assert
        assertNotNull(role.getAppUsers());
        assertTrue(role.getAppUsers().isEmpty());
    }

    @Test
    void appUsers_shouldAcceptNull() {
        // act
        role.setAppUsers(null);

        // assert
        assertNull(role.getAppUsers());
    }

    @Test
    void roleName_shouldAcceptAllEnumValues() {
        // act & assert
        for (RoleName roleName : RoleName.values()) {
            role.setRoleName(roleName);
            assertEquals(roleName, role.getRoleName());
        }
    }

    @Test
    void inheritance_shouldExtendBaseEntity() {
        // assert
        assertInstanceOf(BaseEntity.class, role);
    }

    @Test
    void addingPermission_shouldWork() {
        // arrange
        Set<Permission> permissions = new HashSet<>();
        role.setPermissions(permissions);

        // act
        permissions.add(testPermission);

        // assert
        assertEquals(1, role.getPermissions().size());
        assertTrue(role.getPermissions().contains(testPermission));
    }

    @Test
    void addingAppUser_shouldWork() {
        // arrange
        Set<AppUser> appUsers = new HashSet<>();
        role.setAppUsers(appUsers);
        AppUser appUser = new AppUser();

        // act
        appUsers.add(appUser);

        // assert
        assertEquals(1, role.getAppUsers().size());
        assertTrue(role.getAppUsers().contains(appUser));
    }
}
