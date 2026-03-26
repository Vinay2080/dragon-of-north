package org.miniProjectTwo.DragonOfNorth.infrastructure.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.shared.enums.RoleName;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.miniProjectTwo.DragonOfNorth.shared.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.Instant;

/**
 * Ensures system roles exist when the application starts.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RolesInitializer {

    private final RoleRepository roleRepository;

    /**
     * Creates missing roles defined in {@link RoleName}.
     *
     * @return startup runner that seeds role records
     */
    @Bean
    @Order(1)
    public CommandLineRunner initializeROles() {
        return strings -> {
            String systemUser = "system@Startup";
            Instant now = Instant.now();
            int createdRoles = 0;

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
                    createdRoles++;
                    log.debug("Created missing system role: {}", roleName);
                }
            }
            log.info("Role initialization completed. createdRoles={}, totalRoleTypes={}", createdRoles, RoleName.values().length);
        };
    }
}
