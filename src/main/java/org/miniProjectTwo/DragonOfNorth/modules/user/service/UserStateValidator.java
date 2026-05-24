package org.miniProjectTwo.DragonOfNorth.modules.user.service;

import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.*;

/**
 * Centralized policy engine for account-state-based access decisions across auth and profile flows.
 * Separating lifecycle validation from authentication/authorization keeps security rules consistent and
 * avoids duplicating account-state checks in controllers and services.
 * The validator decides whether a user in a given {@link AppUserStatus} may perform a specific
 * {@link UserLifecycleOperation}, which centralizes transition rules and reduces security gaps.
 * Current enforcement focuses on {@link AppUserStatus#PENDING_VERIFICATION}, {@link AppUserStatus#LOCKED},
 * and {@link AppUserStatus#DELETED}; the structure is intentionally future-ready for additional states
 * (suspended users, temporary locks, MFA-required states, account migrations, admin restrictions,
 * KYC/compliance flows) without scattering logic.
 */
@Component

public class UserStateValidator {

    /**
     * Validates whether a lifecycle operation is permitted for the user's current status.
     * Operations are passed in as {@link UserLifecycleOperation} to keep lifecycle policies centralized
     * and to allow future expansion where operations declare different allowed states.
     * Today most operations are allowed for active users, but the abstraction supports finer-grained
     * differentiation later without changing callers.
     *
     * @param user      the target user
     * @param operation the lifecycle operation being attempted
     * @throws BusinessException when the operation is not allowed for the user's status
     */
    public void validate(AppUser user, UserLifecycleOperation operation) {
        AppUserStatus status = user.getAppUserStatus();
        if (status == null) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, operation.name(), "UNKNOWN");
        }

        if (status == ACTIVE) {
            if (isActiveAllowed(operation)) {
                return;
            }
            throw new BusinessException(ErrorCode.USER_ALREADY_ACTIVE);
        }

        if (status == PENDING_VERIFICATION) {
            if (operation == UserLifecycleOperation.LOCAL_SIGNUP_COMPLETE) {
                return;
            }
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "Email not verified");
        }

        if (status == LOCKED) {
            throw new BusinessException(ErrorCode.USER_BLOCKED);
        }

        if (status == DELETED && isDeletedAllowed(operation)) {
            return;
        }

        if (status == DELETED) {
            throw new BusinessException(ErrorCode.USER_REACTIVATION_REQUIRED);
        }

        throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, operation.name(), status.name());
    }

    /**
     * Returns whether the operation is permitted for active users.
     * Currently, this is mostly permissive because most operations are allowed while active, but the
     * method exists to support stricter per-operation policies as the lifecycle model evolves.
     */
    private boolean isActiveAllowed(UserLifecycleOperation operation) {
        return operation.isActiveAllowed();
    }

    /**
     * Returns whether a deleted account may perform the specified operation.
     * This concentrates reactivation rules in one place and prevents inconsistent handling across flows.
     */
    private boolean isDeletedAllowed(UserLifecycleOperation operation) {
        return switch (operation) {
            case LOCAL_SIGNUP_START, LOCAL_SIGNUP_COMPLETE, GOOGLE_LOGIN, GOOGLE_SIGNUP -> true;
            default -> false;
        };
    }

    public boolean isDeleted(AppUser user) {
        return user.getAppUserStatus() == DELETED;
    }
}
