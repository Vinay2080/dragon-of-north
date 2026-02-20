package org.miniProjectTwo.DragonOfNorth.config.initializer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolesInitializerTest {

    @InjectMocks
    private RolesInitializer rolesInitializer;

    @Mock
    private RoleRepository roleRepository;

    @Test
    void initializeROles_shouldCreateMissingRoles() throws Exception {
        when(roleRepository.existsByRoleName(any())).thenReturn(false);

        CommandLineRunner runner = rolesInitializer.initializeROles();
        runner.run();

        verify(roleRepository, times(RoleName.values().length)).save(any(Role.class));

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, atLeastOnce()).save(roleCaptor.capture());
        assertEquals(true, roleCaptor.getValue().isSystemRole());
    }
}
