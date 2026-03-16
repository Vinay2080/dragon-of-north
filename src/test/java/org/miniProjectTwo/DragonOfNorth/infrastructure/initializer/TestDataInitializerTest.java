package org.miniProjectTwo.DragonOfNorth.infrastructure.initializer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.infrastructure.initializer.TestDataInitializer;
import org.miniProjectTwo.DragonOfNorth.modules.auth.repo.UserAuthProviderRepository;
import org.miniProjectTwo.DragonOfNorth.modules.session.repo.SessionRepository;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

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
    private UserAuthProviderRepository userAuthProviderRepository;


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

        when(appUserRepository.save(any(AppUser.class))).thenAnswer((Answer<AppUser>) invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        testDataInitializer.run();

        verify(appUserRepository, atLeast(10)).save(any(AppUser.class));
    }
}
