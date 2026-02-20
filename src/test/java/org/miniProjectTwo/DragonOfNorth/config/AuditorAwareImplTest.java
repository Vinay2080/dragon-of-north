package org.miniProjectTwo.DragonOfNorth.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AuditorAwareImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    private AuditorAwareImpl auditorAware;

    @BeforeEach
    void setUp() {
        auditorAware = new AuditorAwareImpl(appUserRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentAuditor_shouldReturnSystem_whenNoAuthentication() {
        assertEquals("SYSTEM", auditorAware.getCurrentAuditor().orElseThrow());
    }

    @Test
    void getCurrentAuditor_shouldReturnUsername_whenAuthenticatedUserPresent() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user-1",
                        "pwd",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        );

        assertEquals("user-1", auditorAware.getCurrentAuditor().orElseThrow());
    }
}
