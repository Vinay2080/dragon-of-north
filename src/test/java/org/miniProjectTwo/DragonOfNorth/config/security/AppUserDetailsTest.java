package org.miniProjectTwo.DragonOfNorth.config.security;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Permission;
import org.miniProjectTwo.DragonOfNorth.model.Role;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppUserDetailsTest {

    @Test
    void getAuthorities_shouldIncludeRoleAndPermissions() {
        Permission permission = new Permission();
        permission.setName("session:read");

        Role role = new Role();
        role.setRoleName(RoleName.ADMIN);
        role.setPermissions(Set.of(permission));

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setPassword("secret");
        user.setFailedLoginAttempts(2);
        user.setRoles(Set.of(role));

        AppUserDetails details = new AppUserDetails(user);

        var authorityStrings = details.getAuthorities().stream().map(a -> a.getAuthority()).toList();

        assertTrue(authorityStrings.contains("ROLE_ADMIN"));
        assertTrue(authorityStrings.contains("PERM_session:read"));
        assertEquals(user.getPassword(), details.getPassword());
        assertEquals(user.getId().toString(), details.getUsername());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isEnabled());
    }

    @Test
    void isAccountNonLocked_shouldReturnFalse_whenFailedLoginAttemptsAreHigh() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setFailedLoginAttempts(5);

        AppUserDetails details = new AppUserDetails(user);

        assertFalse(details.isAccountNonLocked());
    }
}
