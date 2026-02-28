package org.miniProjectTwo.DragonOfNorth.exception;

import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;

public class InvalidOAuthTokenException extends BusinessException {

    public InvalidOAuthTokenException() {
        super(ErrorCode.INVALID_OAUTH_TOKEN);
    }

    public InvalidOAuthTokenException(String msg) {
        super(ErrorCode.INVALID_OAUTH_TOKEN, msg);
    }
}
