package org.miniProjectTwo.DragonOfNorth.enums;

import org.miniProjectTwo.DragonOfNorth.config.initializer.TestDataInitializer;
import org.miniProjectTwo.DragonOfNorth.model.Role;

/**
 * User authorization roles controlling API access and system permissions.
 * USER grants basic authentication and profile access. ADMIN provides administrative
 * privileges including user management and system configuration. Critical for
 * security enforcement and access control throughout the application.
 *
 * @see Role for entity mapping
 * @see TestDataInitializer for role assignment
 */
public enum RoleName {
    USER,
    ADMIN
}
