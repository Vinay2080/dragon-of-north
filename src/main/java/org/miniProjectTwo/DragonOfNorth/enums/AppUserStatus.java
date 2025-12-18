package org.miniProjectTwo.DragonOfNorth.enums;

/**
 * Enum representing the possible statuses of a user in the system.
 */
public enum AppUserStatus {

    /**
     * The user is created but not verified yet
     */
    CREATED,
    /**
     * The user is verified for signup.
     */
    VERIFIED,
    /**
     * The user is active and can access the system.
     */
    ACTIVE,
    /**
     * The user has been blocked and cannot access the system.
     */
    BLOCKED,
    /**
     * The user has been deleted from the system.
     */
    DELETED
}
