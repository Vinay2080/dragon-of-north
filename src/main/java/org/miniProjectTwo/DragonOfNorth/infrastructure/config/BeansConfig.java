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
 * Registers foundational beans consumed by multiple modules at runtime.
 * <p>
 * Dependency chain:
 * <ul>
 *   <li>{@link org.springframework.data.domain.AuditorAware} is consumed by JPA auditing via {@code JpaConfig}.</li>
 *   <li>{@link ObjectMapper} is used by HTTP serialization paths and internal JSON codecs.</li>
 * </ul>
 * Modifying naming strategy or auditor resolution semantics can affect persistence history,
 * API contracts, and audit/compliance traces across the system.
 */
@Configuration
@RequiredArgsConstructor
public class BeansConfig {

    private final AppUserRepository appUserRepository;

    /**
     * Provides the auditor resolver used by Spring Data JPA.
     * <p>The auditor-aware implementation checks the Spring Security context for an authenticated principal and extracts a user identifier to use as the auditor. It supports principals of type {@link org.miniProjectTwo.DragonOfNorth.security.model.AppUserDetails}, UUID, or String. If no authenticated user is found, it defaults to "SYSTEM".</p>
     * @return auditor provider for {@code createdBy}/{@code updatedBy} fields
     */
    @Bean
    @NullMarked
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl(appUserRepository);
    }

    /**
     * Provides a shared {@link ObjectMapper} for API payloads.
     * <blockquote>
     * <p>The object mapper is configured with a snake_case naming strategy, which means that JSON properties will be serialized and deserialized using snake_case (e.g., "created_at" instead of "createdAt"). This is a common convention for JSON APIs and ensures consistency in the API contracts.</p>
     * <p>Additionally, the object mapper registers the {@link JavaTimeModule}, which provides support for Java 8 date and time types (e.g., LocalDate, LocalDateTime). This allows for proper serialization and deserialization of date/time fields in API payloads, ensuring that they are correctly formatted and parsed according to the ISO-8601 standard.</p>
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
