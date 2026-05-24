package org.miniProjectTwo.DragonOfNorth.shared.enums;

import lombok.Getter;

/**
 * Describes lifecycle-relevant operations that may be gated by account state.
 * Using a dedicated operation enum allows {@link org.miniProjectTwo.DragonOfNorth.modules.user.service.UserStateValidator}
 * to centralize lifecycle rules, keeping access decisions consistent across controllers and services.
 * The current model treats most operations as active-allowed, but this structure supports future
 * state-aware policies (suspensions, temporary locks, MFA-required states, admin restrictions,
 * account migration, KYC/compliance flows) without changing callers.
 */
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
    SESSION_ROTATE_REFRESH(true),
    PASSWORDLESS_LOGIN_REQUEST(true),
    PASSWORDLESS_LOGIN_VERIFY(true),
    MFA_SETUP_REQUEST(true),
    MFA_SETUP_CONFIRM(true);

    /**
     * Indicates whether the operation is permitted for active users.
     * This is currently permissive for most operations but remains extensible for finer-grained policies.
     */
    private final boolean activeAllowed;

    UserLifecycleOperation(boolean activeAllowed) {
        this.activeAllowed = activeAllowed;
    }

}