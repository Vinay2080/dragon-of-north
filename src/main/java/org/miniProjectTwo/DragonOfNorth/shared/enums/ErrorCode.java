package org.miniProjectTwo.DragonOfNorth.shared.enums;

import lombok.Getter;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ErrorResponse;
import org.miniProjectTwo.DragonOfNorth.shared.exception.ApplicationExceptionHandler;
import org.springframework.http.HttpStatus;

/**
 * Centralized error code mapping for consistent API error responses.
 * Each error maps to specific HTTP status codes and provides structured error
 * information for client handling. Supports parameterized messages for dynamic
 * error details. Critical for maintaining API contract and error handling consistency.
 *
 * @see ErrorResponse for response structure
 * @see ApplicationExceptionHandler for error mapping
 */
@Getter
public enum ErrorCode {
    INVALID_TOKEN("TOK_001", "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED),
    MALFORMED_TOKEN("TOK_002", "Malformed JWT token", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_TOKEN("TOK_003", "Unsupported JWT token", HttpStatus.BAD_REQUEST),
    ILLEGAL_TOKEN("TOK_004", "Illegal JWT token", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("TOK_005", "Empty refresh token", HttpStatus.UNAUTHORIZED),

    IDENTIFIER_MISMATCH("AUTH_001", "%s does not matches identifier type", HttpStatus.BAD_REQUEST),
    TOO_MANY_REQUESTS("AUTH_002", "too many requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    RATE_LIMIT_EXCEEDED("AUTH_003", "Too many requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    STATUS_MISMATCH("AUTH_004", "Invalid status expected status %s", HttpStatus.CONFLICT),
    AUTHENTICATION_FAILED("AUTH_005", "Invalid username or password", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_006", "Access is denied", HttpStatus.FORBIDDEN),
    CSRF_INVALID("AUTH_007", "Invalid or missing CSRF token", HttpStatus.FORBIDDEN),
    PASSWORD_CHANGE_NOT_ALLOWED("AUTH_008", "Password change not allowed for Google accounts", HttpStatus.FORBIDDEN),
    INVALID_CURRENT_PASSWORD("AUTH_009", "Current password is incorrect", HttpStatus.BAD_REQUEST),
    WEAK_PASSWORD("AUTH_010", "Password must be at least 8 characters with letters and numbers", HttpStatus.BAD_REQUEST),
    SAME_PASSWORD("AUTH_011", "New password must be different from current password", HttpStatus.BAD_REQUEST),

    MFA_ALREADY_ENABLED("AUTH_012", "MFA is already enabled", HttpStatus.CONFLICT),
    MFA_SETUP_EXPIRED("AUTH_013", "MFA setup session has expired. Please request again.", HttpStatus.BAD_REQUEST),
    MFA_REQUIRED("AUTH_014", "MFA is required for this operation", HttpStatus.FORBIDDEN),
    MFA_INVALID_CODE("AUTH_015", "Invalid MFA code", HttpStatus.BAD_REQUEST),
    MFA_SETUP_REQUEST_LIMIT("AUTH_016", "Too many MFA setup requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    MFA_SETUP_REQUEST_FAILED("AUTH_017", "MFA setup request failed", HttpStatus.INTERNAL_SERVER_ERROR),
    MFA_SETUP_CONFIRM_FAILED("AUTH_018", "MFA setup confirmation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    MFA_SETUP_NOT_FOUND("AUTH_019", "MFA setup not found", HttpStatus.NOT_FOUND),
    MFA_SETUP_EXPIRED_OR_USED("AUTH_020", "MFA setup has expired or already been used", HttpStatus.BAD_REQUEST),
    MFA_CHALLENGE_FAILED("AUTH_021", "MFA challenge failed", HttpStatus.BAD_REQUEST),
    MFA_CHALLENGE_EXPIRED_OR_MISSING("AUTH_022", "MFA challenge has expired or is missing", HttpStatus.BAD_REQUEST),
    MFA_CHALLENGE_BUSY_OR_REPLAY("AUTH_023", "MFA challenge is busy or replayed", HttpStatus.BAD_REQUEST),
    MFA_CHALLENGE_CONSUME_RACE("AUTH_024", "MFA challenge consume race condition", HttpStatus.BAD_REQUEST),
    MFA_CHALLENGE_LOCKED_OUT("AUTH_025", "MFA challenge is locked out", HttpStatus.BAD_REQUEST),
    MFA_CHALLENGE_NOT_FOUND("AUTH_026", "MFA challenge not found", HttpStatus.NOT_FOUND),
    MFA_CHALLENGE_ALREADY_CONSUMED("AUTH_027", "MFA challenge has already been consumed", HttpStatus.BAD_REQUEST),
    MFA_STEP_UP_REQUIRED("AUTH_028", "Recent MFA verification required. Please complete step-up MFA before proceeding.", HttpStatus.FORBIDDEN),
    MFA_CONFIGURATION_INVALID("AUTH_029", "MFA is enabled but no MFA providers are configured for this account", HttpStatus.FORBIDDEN),
    MFA_CHALLENGE_INFRASTRUCTURE_UNAVAILABLE("AUTH_030", "MFA challenge service temporarily unavailable. Please try again.", HttpStatus.SERVICE_UNAVAILABLE),

    ROLE_NOT_FOUND("ROL_009", "role %s not found", HttpStatus.NOT_FOUND),

    USER_NOT_FOUND("USER_001", "user not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_VERIFIED("USER_002", "user is already verified", HttpStatus.CONFLICT),
    USER_OPERATION_NOT_ALLOWED("USER_003", "Operation %s is not allowed for account status %s", HttpStatus.FORBIDDEN),
    USER_REACTIVATION_REQUIRED("USER_004", "Account is deleted. Complete verification to reactivate", HttpStatus.FORBIDDEN),
    USER_BLOCKED("USER_005", "Account is blocked", HttpStatus.LOCKED),
    USER_ALREADY_ACTIVE("USER_006", "User is already active", HttpStatus.CONFLICT),
    USER_INACTIVE("USER_007", "User account is not active", HttpStatus.UNAUTHORIZED),

    INVALID_INPUT("VAL_001", "invalid input", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED("VAL_002", "email not verified", HttpStatus.UNAUTHORIZED),
    PHONE_NOT_VERIFIED("VAL_003", "phone not verified", HttpStatus.UNAUTHORIZED),
    OTP_VERIFICATION_REQUIRED("VAL_004", "Verification required before completing signup", HttpStatus.BAD_REQUEST),

    OTP_RATE_LIMIT("OTP_001", "wait %s seconds before requesting another OTP for %s", HttpStatus.TOO_MANY_REQUESTS),
    OTP_TOO_MANY_REQUESTS("OTP_002", "Too many otp requests. Blocked for %s minutes.", HttpStatus.TOO_MANY_REQUESTS),
    OTP_NOT_FOUND("OTP_003", "OTP not found", HttpStatus.NOT_FOUND),

    INVALID_OAUTH_TOKEN("OAUTH_001", "Invalid OAuth token", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS("OAUTH_002", "email is already associated with another OAuth provider", HttpStatus.CONFLICT),
    OAUTH_LINK_CONFIRMATION_REQUIRED("OAUTH_004", "Account exists. Login with password before linking Google", HttpStatus.CONFLICT),
    USER_CREATION_FAILED("OAUTH_003", "User creation failed", HttpStatus.INTERNAL_SERVER_ERROR),

    PROFILE_ALREADY_EXISTS("PROFILE_001", "Profile already exists", HttpStatus.CONFLICT),
    USERNAME_ALREADY_TAKEN("PROFILE_002", "Profile name already in use", HttpStatus.CONFLICT);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code,
              String defaultMessage,
              HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
