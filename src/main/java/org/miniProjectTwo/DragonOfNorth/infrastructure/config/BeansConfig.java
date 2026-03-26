package org.miniProjectTwo.DragonOfNorth.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.infrastructure.audit.AuditorAwareImpl;
import org.miniProjectTwo.DragonOfNorth.modules.user.repo.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

/**
 * Registers shared infrastructure beans.
 *
 * <p>Includes JPA auditing support and common JSON serialization settings.</p>
 */
@Configuration
@RequiredArgsConstructor
public class BeansConfig {

    private final AppUserRepository appUserRepository;

    /**
     * Provides the auditor resolver used by Spring Data JPA.
     *
     * @return auditor provider for {@code createdBy}/{@code updatedBy} fields
     */
    @Bean
    @NullMarked
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl(appUserRepository);
    }

    /**
     * Provides a shared {@link ObjectMapper} for API payloads.
     *
     * @return mapper configured with snake_case names and Java time support
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
