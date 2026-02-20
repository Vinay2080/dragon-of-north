package org.miniProjectTwo.DragonOfNorth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.repositories.AppUserRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class BeansConfigTest {

    @Mock
    private AppUserRepository appUserRepository;
    private final BeansConfig beansConfig = new BeansConfig(appUserRepository);

    @Test
    void auditorAware_shouldReturnAuditorAwareImpl() {
        AuditorAware<String> auditorAware = beansConfig.auditorAware();
        assertNotNull(auditorAware);
        assertInstanceOf(AuditorAwareImpl.class, auditorAware);
    }

    @Test
    void objectMapper_shouldReturnNonNullMapper() {
        ObjectMapper mapper = beansConfig.objectMapper();
        assertNotNull(mapper);
    }
}
