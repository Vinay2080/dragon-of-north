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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RolesInitializer {

    private final RoleRepository roleRepository;

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
