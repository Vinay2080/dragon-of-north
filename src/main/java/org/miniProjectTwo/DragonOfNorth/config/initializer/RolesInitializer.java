package org.miniProjectTwo.DragonOfNorth.config.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.miniProjectTwo.DragonOfNorth.repositories.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.Instant;

/**
 * Configuration component that initializes system roles on application startup.
 * Ensures all required roles defined in {@link RoleName} enum exist in the database
 * before the application starts serving requests. Runs with the highest precedence (@Order(1))
 * to guarantee roles are available for user registration and authentication.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RolesInitializer {

    private final RoleRepository roleRepository;

    /**
     * CommandLineRunner bean that executes on application startup.
     * Creates any missing system roles in the database with audit information.
     * Uses "system@Startup" as the creator and marks roles as system-managed.
     *
     * @return CommandLineRunner that initializes roles
     */
    @Bean
    @Order(1)
    public CommandLineRunner initializeROles() {
        return strings -> {
            log.info("Checking and initializing roles in the database");

            String systemUser = "system@Startup";
            Instant now = Instant.now();

            for (RoleName roleName : RoleName.values()) {
                if (!roleRepository.existsByRoleName(roleName)) {
                    Role role = new Role();

                    role.setRoleName(roleName);
                    role.setSystemRole(true);

                    role.setCreatedAt(now);
                    role.setUpdatedAt(now);
                    role.setCreatedBy(systemUser);
                    role.setDeleted(false);

                    roleRepository.save(role);
                    log.info("Created role: {}", roleName);
                }
            }
            log.info("Role initialization completed");
        };
    }
}
