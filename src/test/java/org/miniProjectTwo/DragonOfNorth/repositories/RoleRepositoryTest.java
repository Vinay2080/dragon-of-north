package org.miniProjectTwo.DragonOfNorth.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleRepositoryTest {

    @Mock
    private RoleRepository roleRepository;

    @Test
    void findByRoleName_shouldReturnRole_whenRoleExists() {
        // arrange
        RoleName roleName = RoleName.USER;
        Role expectedRole = createTestRole(roleName);

        when(roleRepository.findByRoleName(roleName)).thenReturn(Optional.of(expectedRole));

        // act
        Optional<Role> result = roleRepository.findByRoleName(roleName);

        // assert
        assertTrue(result.isPresent());
        assertEquals(expectedRole, result.get());
        assertEquals(roleName, result.get().getRoleName());
        verify(roleRepository).findByRoleName(roleName);
    }

    private Role createTestRole(RoleName roleName) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setRoleName(roleName);
        role.setSystemRole(roleName == RoleName.USER);
        return role;
    }

    @Test
    void findByRoleName_shouldReturnEmpty_whenRoleDoesNotExist() {
        // arrange
        RoleName roleName = RoleName.USER;

        when(roleRepository.findByRoleName(roleName)).thenReturn(Optional.empty());

        // act
        Optional<Role> result = roleRepository.findByRoleName(roleName);

        // assert
        assertFalse(result.isPresent());
        verify(roleRepository).findByRoleName(roleName);
    }

    @Test
    void existsByRoleName_shouldReturnTrue_whenRoleExists() {
        // arrange
        RoleName roleName = RoleName.USER;

        when(roleRepository.existsByRoleName(roleName)).thenReturn(true);

        // act
        boolean result = roleRepository.existsByRoleName(roleName);

        // assert
        assertTrue(result);
        verify(roleRepository).existsByRoleName(roleName);
    }

    @Test
    void existsByRoleName_shouldReturnFalse_whenRoleDoesNotExist() {
        // arrange
        RoleName roleName = RoleName.ADMIN;

        when(roleRepository.existsByRoleName(roleName)).thenReturn(false);

        // act
        boolean result = roleRepository.existsByRoleName(roleName);

        // assert
        assertFalse(result);
        verify(roleRepository).existsByRoleName(roleName);
    }

    @Test
    void findByRoleName_shouldWorkForAdminRole() {
        // arrange
        RoleName roleName = RoleName.ADMIN;
        Role expectedRole = createTestRole(roleName);

        when(roleRepository.findByRoleName(roleName)).thenReturn(Optional.of(expectedRole));

        // act
        Optional<Role> result = roleRepository.findByRoleName(roleName);

        // assert
        assertTrue(result.isPresent());
        assertEquals(RoleName.ADMIN, result.get().getRoleName());
        verify(roleRepository).findByRoleName(roleName);
    }

    @Test
    void existsByRoleName_shouldWorkForSystemRole() {
        // arrange
        RoleName roleName = RoleName.USER;

        when(roleRepository.existsByRoleName(roleName)).thenReturn(true);

        // act
        boolean result = roleRepository.existsByRoleName(roleName);

        // assert
        assertTrue(result);
        verify(roleRepository).existsByRoleName(roleName);
    }
}
