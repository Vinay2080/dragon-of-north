package org.miniProjectTwo.DragonOfNorth.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom runtime exception representing a business-level error in the application.
 *
 * <p>This exception wraps an {@link ErrorCode} and optional arguments used for
 * formatting the error message. It supports two usage patterns:
 * <ul>
 *     <li>Errors that use simple fixed messages</li>
 *     <li>Errors that require dynamic placeholders (e.g., {@code %s})</li>
 * </ul>
 *
 * <p>Designed for high-performance scenarios where varargs allocation is avoided.
 * Message formatting is only attempted when arguments are provided.</p>
 */
@Getter
@Slf4j
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object args;

    /**
     * Creates a business exception with a dynamic message that requires arguments.
     *
     * @param errorCode the error type
     * @param args      a single argument to be injected into the error message format
     */
    public BusinessException(ErrorCode errorCode, Object args) {
        super(format(errorCode, args));
        this.errorCode = errorCode;
        this.args = args;

        log.debug("BusinessException created: code={}, args={}", errorCode, args);
    }

    /**
     * Creates a business exception with a static message.
     *
     * @param errorCode the error type
     */
    public BusinessException(ErrorCode errorCode) {
        super(format(errorCode, null));
        this.errorCode = errorCode;
        this.args = null;

        log.debug("BusinessException created: code={} (no args)", errorCode);
    }

    /**
     * Formats the error message using {@link String#format(String, Object...)}.
     *
     * <p>Formatting is only applied if an argument is provided. If formatting fails
     * due to mismatched placeholders, the raw default message is returned to avoid
     * disrupting the application flow.</p>
     *
     * @param errorCode the error definition
     * @param args      optional argument used for formatting
     * @return formatted or raw message
     */
    private static String format(ErrorCode errorCode, Object args) {
        String msg = errorCode.getDefaultMessage();

        if (args == null) {
            return msg;
        }

        try {
            return String.format(msg, args);
        } catch (Exception ex) {
            return msg; // fail-safe fallback
        }
    }

}
