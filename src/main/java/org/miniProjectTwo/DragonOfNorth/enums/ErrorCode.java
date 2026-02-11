package org.miniProjectTwo.DragonOfNorth.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

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

    ROLE_NOT_FOUND("ROL_009", "role %s not found", HttpStatus.NOT_FOUND),

    USER_NOT_FOUND("USER_001", "user not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_VERIFIED("USER_002", "user is already verified", HttpStatus.CONFLICT),

    INVALID_INPUT("VAL_001", "invalid input", HttpStatus.BAD_REQUEST),

    OTP_RATE_LIMIT("OTP_001", "wait %s seconds before requesting another OTP for %s", HttpStatus.TOO_MANY_REQUESTS),
    OTP_TOO_MANY_REQUESTS("OTP_002", "Too many otp requests. Blocked for %s minutes.", HttpStatus.TOO_MANY_REQUESTS);

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
