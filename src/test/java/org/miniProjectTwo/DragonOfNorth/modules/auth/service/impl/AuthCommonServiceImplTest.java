package org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.auth.service.impl.AuthCommonServiceImpl;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.ACTIVE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.LOCKED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCommonServiceImplTest {

    @InjectMocks
    private AuthCommonServiceImpl authCommonService;

    @Mock
    private RoleRepository roleRepository;

    @Test
    void assignDefaultRole_shouldAssignUserRole_whenUserHasNoRoles() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setRoles(new HashSet<>());
        Role userRole = new Role();
        userRole.setRoleName(RoleName.USER);

        when(roleRepository.findByRoleName(RoleName.USER)).thenReturn(Optional.of(userRole));

        // act
        authCommonService.assignDefaultRole(appUser);

        // assert
        assertEquals(1, appUser.getRoles().size());
        assertTrue(appUser.getRoles().contains(userRole));

        // verify
        verify(roleRepository).findByRoleName(RoleName.USER);
    }

    @Test
    void assignDefaultRole_shouldNotAssignRole_whenUserAlreadyHasRoles() {
        // arrange
        AppUser appUser = new AppUser();
        Role existingRole = new Role();
        existingRole.setRoleName(RoleName.ADMIN);
        appUser.setRoles(Set.of(existingRole));

        // act
        authCommonService.assignDefaultRole(appUser);

        // assert
        assertEquals(1, appUser.getRoles().size());
        assertTrue(appUser.getRoles().contains(existingRole));

        // verify
        verify(roleRepository, never()).findByRoleName(any());
    }

    @Test
    void assignDefaultRole_shouldThrowException_whenRoleNotFound() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setRoles(new HashSet<>());

        when(roleRepository.findByRoleName(RoleName.USER)).thenReturn(Optional.empty());

        // act & assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authCommonService.assignDefaultRole(appUser));
        assertEquals(ErrorCode.ROLE_NOT_FOUND, exception.getErrorCode());

        // verify
        verify(roleRepository).findByRoleName(RoleName.USER);
    }

    @Test
    void updateUserStatus_shouldSetProvidedStatus() {
        // arrange
        AppUser appUser = new AppUser();
        appUser.setAppUserStatus(ACTIVE);

        // act
        authCommonService.updateUserStatus(LOCKED, appUser);

        // assert
        assertEquals(LOCKED, appUser.getAppUserStatus());
    }
}
