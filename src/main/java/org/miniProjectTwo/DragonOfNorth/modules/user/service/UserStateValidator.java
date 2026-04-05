package org.miniProjectTwo.DragonOfNorth.modules.user.service;

import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import static org.miniProjectTwo.DragonOfNorth.modules.user.service.UserLifecycleOperation.LOCAL_LOGIN;
import static org.miniProjectTwo.DragonOfNorth.modules.user.service.UserLifecycleOperation.LOCAL_SIGNUP_COMPLETE;
import static org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus.*;

@Component
public class UserStateValidator {

    public void validate(AppUser user, UserLifecycleOperation operation) {
        AppUserStatus status = user.getAppUserStatus();
        if (status == null) {
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, operation.name(), "UNKNOWN");
        }

        if (status == PENDING_VERIFICATION) {
            if (operation == LOCAL_SIGNUP_COMPLETE) {
                return;
            }
            if (operation == LOCAL_LOGIN) {
                throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED, "Email not verified");
            }
            throw new BusinessException(ErrorCode.USER_OPERATION_NOT_ALLOWED, operation.name(), status.name());
        }

        if (status == ACTIVE && isActiveAllowed(operation)) {
            return;
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

    private boolean isActiveAllowed(UserLifecycleOperation operation) {
        return operation.isActiveAllowed();
    }

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
