package org.miniProjectTwo.DragonOfNorth.modules.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.shared.enums.UserLifecycleOperation;
import org.miniProjectTwo.DragonOfNorth.shared.exception.BusinessException;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserStateValidatorTest {

    @InjectMocks
    private UserStateValidator userStateValidator;

    private AppUser userWithStatus(AppUserStatus status) {
        AppUser user = new AppUser();
        user.setAppUserStatus(status);
        return user;
    }

    // ── ACTIVE ──────────────────────────────────────────────────────────────

    @Test
    void validate_shouldPass_whenStatusIsActive_andOperationAllowed() {
        AppUser user = userWithStatus(AppUserStatus.ACTIVE);
        assertDoesNotThrow(() -> userStateValidator.validate(user, UserLifecycleOperation.LOCAL_LOGIN));
    }

    @Test
    void validate_shouldPass_whenStatusIsActive_andSignupStart() {
        AppUser user = userWithStatus(AppUserStatus.ACTIVE);
        // LOCAL_SIGNUP_START has activeAllowed= true, so it is permitted for ACTIVE users
        assertDoesNotThrow(() -> userStateValidator.validate(user, UserLifecycleOperation.LOCAL_SIGNUP_START));
    }

    // ── PENDING_VERIFICATION ────────────────────────────────────────────────

    @Test
    void validate_shouldPass_whenStatusIsPendingVerification_andSignupComplete() {
        AppUser user = userWithStatus(AppUserStatus.PENDING_VERIFICATION);
        assertDoesNotThrow(() -> userStateValidator.validate(user, UserLifecycleOperation.LOCAL_SIGNUP_COMPLETE));
    }

    @Test
    void validate_shouldThrowEmailNotVerified_whenPendingAndOtherOperation() {
        AppUser user = userWithStatus(AppUserStatus.PENDING_VERIFICATION);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userStateValidator.validate(user, UserLifecycleOperation.LOCAL_LOGIN));
        assertEquals(ErrorCode.EMAIL_NOT_VERIFIED, ex.getErrorCode());
    }

    // ── LOCKED ──────────────────────────────────────────────────────────────

    @Test
    void validate_shouldThrowUserBlocked_whenStatusIsLocked() {
        AppUser user = userWithStatus(AppUserStatus.LOCKED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userStateValidator.validate(user, UserLifecycleOperation.LOCAL_LOGIN));
        assertEquals(ErrorCode.USER_BLOCKED, ex.getErrorCode());
    }

    // ── DELETED ─────────────────────────────────────────────────────────────

    @Test
    void validate_shouldPass_whenStatusIsDeleted_andLocalSignupStart() {
        AppUser user = userWithStatus(AppUserStatus.DELETED);
        assertDoesNotThrow(() -> userStateValidator.validate(user, UserLifecycleOperation.LOCAL_SIGNUP_START));
    }

    @Test
    void validate_shouldPass_whenStatusIsDeleted_andGoogleLogin() {
        AppUser user = userWithStatus(AppUserStatus.DELETED);
        assertDoesNotThrow(() -> userStateValidator.validate(user, UserLifecycleOperation.GOOGLE_LOGIN));
    }

    @Test
    void validate_shouldThrowUserReactivationRequired_whenDeletedAndOtherOperation() {
        AppUser user = userWithStatus(AppUserStatus.DELETED);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userStateValidator.validate(user, UserLifecycleOperation.LOCAL_LOGIN));
        assertEquals(ErrorCode.USER_REACTIVATION_REQUIRED, ex.getErrorCode());
    }

    // ── NULL STATUS ─────────────────────────────────────────────────────────

    @Test
    void validate_shouldThrowUserOperationNotAllowed_whenStatusIsNull() {
        AppUser user = userWithStatus(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userStateValidator.validate(user, UserLifecycleOperation.LOCAL_LOGIN));
        assertEquals(ErrorCode.USER_OPERATION_NOT_ALLOWED, ex.getErrorCode());
    }

    // ── isDeleted ────────────────────────────────────────────────────────────

    @Test
    void isDeleted_shouldReturnTrue_whenStatusIsDeleted() {
        AppUser user = userWithStatus(AppUserStatus.DELETED);
        assertTrue(userStateValidator.isDeleted(user));
    }

    @Test
    void isDeleted_shouldReturnFalse_whenStatusIsActive() {
        AppUser user = userWithStatus(AppUserStatus.ACTIVE);
        assertFalse(userStateValidator.isDeleted(user));
    }

    @Test
    void isDeleted_shouldReturnFalse_whenStatusIsPendingVerification() {
        AppUser user = userWithStatus(AppUserStatus.PENDING_VERIFICATION);
        assertFalse(userStateValidator.isDeleted(user));
    }
}
