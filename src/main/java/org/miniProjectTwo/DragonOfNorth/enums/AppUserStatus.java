package org.miniProjectTwo.DragonOfNorth.enums;

/**
 * Enum representing the possible statuses of a user in the system.
 */
public enum AppUserStatus {

    NOT_EXIST,
    /**
     * The user is created but not verified yet
     */
    CREATED,
    /**
     * The user is verified for signup.
     */
    VERIFIED,
    /**
     * The user has been deleted from the system.
     */
    DELETED
}
