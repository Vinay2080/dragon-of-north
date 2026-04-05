package org.miniProjectTwo.DragonOfNorth.shared.enums;

import lombok.Getter;

@Getter
public enum UserLifecycleOperation {
    LOCAL_LOGIN(true),
    LOCAL_SIGNUP_START(true),
    LOCAL_SIGNUP_COMPLETE(true),
    GOOGLE_LOGIN(true),
    GOOGLE_SIGNUP(true),
    PASSWORD_RESET_REQUEST(true),
    PASSWORD_RESET_CONFIRM(true),
    PASSWORD_CHANGE(true),
    PROFILE_READ(true),
    PROFILE_UPDATE(true),
    ACCOUNT_DELETION(true),
    SESSION_REVOKE_CURRENT(true),
    SESSION_REVOKE_BY_ID(true),
    SESSION_REVOKE_OTHERS(true),
    SESSION_ROTATE_REFRESH(true);

    private final boolean activeAllowed;

    UserLifecycleOperation(boolean activeAllowed) {
        this.activeAllowed = activeAllowed;
    }

}