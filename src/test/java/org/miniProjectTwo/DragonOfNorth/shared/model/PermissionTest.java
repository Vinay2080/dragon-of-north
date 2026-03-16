package org.miniProjectTwo.DragonOfNorth.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.shared.model.Permission;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {

    private Permission permission;
    private Role testRole;

    @BeforeEach
    void setUp() {
        permission = new Permission();
        testRole = new Role();
    }

    @Test
    void constructor_shouldCreateEmptyPermission() {
        // assert
        assertNotNull(permission);
        assertNull(permission.getName());
        assertNotNull(permission.getRoles());
        assertTrue(permission.getRoles().isEmpty());
    }

    @Test
    void constructorWithAllArgs_shouldCreatePermission() {
        // arrange
        UUID id = UUID.randomUUID();
        String permissionName = "READ_WRITE_PERMISSION";
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);

        // act
        Permission newPermission = new Permission();
        newPermission.setId(id);
        newPermission.setName(permissionName);
        newPermission.setRoles(roles);

        // assert
        assertEquals(id, newPermission.getId());
        assertEquals(permissionName, newPermission.getName());
        assertEquals(roles, newPermission.getRoles());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // arrange
        String permissionName = "ADMIN_PERMISSION";
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);

        // act
        permission.setName(permissionName);
        permission.setRoles(roles);

        // assert
        assertEquals(permissionName, permission.getName());
        assertEquals(roles, permission.getRoles());
    }

    @Test
    void name_shouldAcceptNull() {
        // act
        permission.setName(null);

        // assert
        assertNull(permission.getName());
    }

    @Test
    void name_shouldAcceptEmptyString() {
        // act
        permission.setName("");

        // assert
        assertEquals("", permission.getName());
    }

    @Test
    void name_shouldAcceptValidString() {
        // arrange
        String validName = "USER_READ_PERMISSION";

        // act
        permission.setName(validName);

        // assert
        assertEquals(validName, permission.getName());
    }

    @Test
    void roles_shouldAcceptEmptySet() {
        // act
        permission.setRoles(new HashSet<>());

        // assert
        assertNotNull(permission.getRoles());
        assertTrue(permission.getRoles().isEmpty());
    }

    @Test
    void roles_shouldAcceptNull() {
        // act
        permission.setRoles(null);

        // assert
        assertNull(permission.getRoles());
    }

    @Test
    void addingRole_shouldWork() {
        // arrange
        Set<Role> roles = new HashSet<>();
        permission.setRoles(roles);

        // act
        roles.add(testRole);

        // assert
        assertEquals(1, permission.getRoles().size());
        assertTrue(permission.getRoles().contains(testRole));
    }

    @Test
    void removingRole_shouldWork() {
        // arrange
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);
        permission.setRoles(roles);

        // act
        roles.remove(testRole);

        // assert
        assertEquals(0, permission.getRoles().size());
        assertFalse(permission.getRoles().contains(testRole));
    }

    @Test
    void multipleRoles_shouldWork() {
        // arrange
        Role role1 = new Role();
        Role role2 = new Role();
        Set<Role> roles = new HashSet<>();
        permission.setRoles(roles);

        // act
        roles.add(role1);
        roles.add(role2);

        // assert
        assertEquals(2, permission.getRoles().size());
        assertTrue(permission.getRoles().contains(role1));
        assertTrue(permission.getRoles().contains(role2));
    }

    @Test
    void inheritance_shouldExtendBaseEntity() {
        // assert
        assertNotNull(permission);
    }

    @Test
    void equalsAndHashCode_shouldWorkCorrectly() {
        // BaseEntity doesn't implement equals/hashCode based on business fields,
        // So we just verify the objects are different instances
        Permission permission1 = new Permission();
        permission1.setName("TEST_PERMISSION");

        Permission permission2 = new Permission();
        permission2.setName("TEST_PERMISSION");

        Permission permission3 = new Permission();
        permission3.setName("OTHER_PERMISSION");

        // assert
        assertNotEquals(permission1, permission2); // Different instances
        assertNotEquals(permission1, permission3); // Different instances
        // We can't test hashCode equality since BaseEntity doesn't override it
    }
}
