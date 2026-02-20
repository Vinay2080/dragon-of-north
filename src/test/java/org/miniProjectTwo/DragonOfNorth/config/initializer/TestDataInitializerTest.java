package org.miniProjectTwo.DragonOfNorth.config.initializer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.miniProjectTwo.DragonOfNorth.repositories.SessionRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestDataInitializerTest {

    @InjectMocks
    private TestDataInitializer testDataInitializer;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void run_shouldSeedUsersAndSessions_whenRolesExist() {
        Role userRole = new Role();
        userRole.setRoleName(RoleName.USER);

        Role adminRole = new Role();
        adminRole.setRoleName(RoleName.ADMIN);

        when(roleRepository.findByRoleName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(roleRepository.findByRoleName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        when(appUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(appUserRepository.findByPhone(anyString())).thenReturn(Optional.empty());
        when(sessionRepository.findByAppUserAndDeviceId(any(), anyString())).thenReturn(Optional.empty());

        AppUser seededUser = new AppUser();
        when(appUserRepository.save(any(AppUser.class))).thenReturn(seededUser);

        testDataInitializer.run();

        verify(appUserRepository, atLeast(10)).save(any(AppUser.class));
    }
}
