package org.miniProjectTwo.DragonOfNorth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_TOKEN("TOK_001", "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED),
    MALFORMED_TOKEN("TOK_002", "Malformed JWT token", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_TOKEN("TOK_003", "Unsupported JWT token", HttpStatus.BAD_REQUEST),
    ILLEGAL_TOKEN("TOK_004", "Illegal JWT token", HttpStatus.BAD_REQUEST),

    IDENTIFIER_MISMATCH("AUTH_001", "%s does not matches identifier type", HttpStatus.BAD_REQUEST),
    TOO_MANY_REQUESTS("AUTH_002", "too many requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    STATUS_MISMATCH("AUTH_003", "Invalid status expected status %s", HttpStatus.CONFLICT),

    ROLE_NOT_FOUND("ROL_009", "role %s not found", HttpStatus.NOT_FOUND),

    USER_NOT_FOUND("USER_001", "user not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_VERIFIED("USER_002","user is already verified" ,HttpStatus.CONFLICT );

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
