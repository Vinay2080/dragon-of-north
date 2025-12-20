package org.miniProjectTwo.DragonOfNorth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_TOKEN("AUTH_001", "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED),
    MALFORMED_TOKEN("AUTH_002", "Malformed JWT token", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_TOKEN("AUTH_003", "Unsupported JWT token", HttpStatus.BAD_REQUEST),
    ILLEGAL_TOKEN("AUTH_004", "Illegal JWT token", HttpStatus.BAD_REQUEST),
    IDENTIFIER_MISMATCH("AUTH_005", "%s does not matches identifier type", HttpStatus.BAD_REQUEST);

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
