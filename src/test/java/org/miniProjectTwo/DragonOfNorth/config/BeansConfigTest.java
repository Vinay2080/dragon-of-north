package org.miniProjectTwo.DragonOfNorth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;

import static org.junit.jupiter.api.Assertions.*;

class BeansConfigTest {

    private final BeansConfig beansConfig = new BeansConfig();

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
