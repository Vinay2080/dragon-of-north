package org.miniProjectTwo.DragonOfNorth.shared.exception;

import org.miniProjectTwo.DragonOfNorth.shared.enums.ErrorCode;

public class InvalidOAuthTokenException extends BusinessException {

    public InvalidOAuthTokenException() {
        super(ErrorCode.INVALID_OAUTH_TOKEN);
    }

    public InvalidOAuthTokenException(String ignoredMsg) {
        super(ErrorCode.INVALID_OAUTH_TOKEN);
    }
}
