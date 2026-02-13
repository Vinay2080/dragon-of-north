package org.miniProjectTwo.DragonOfNorth.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;

/**
 * Business logic exception with structured error codes and dynamic messaging.
 * <p>
 * Wraps ErrorCode for consistent API responses and supports parameterized messages.
 * Performance-optimized with lazy message formatting. Critical for business rule
 * enforcement and standardized error reporting across services.
 *
 * @see ErrorCode for error definitions
 * @see ApplicationExceptionHandler for error mapping
 */
@Getter
@Slf4j
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    /**
     * Creates a business exception with a dynamic message that requires arguments.
     *
     * @param errorCode the error type
     * @param args      a single argument to be injected into the error message format
     */
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(format(errorCode, args));
        this.errorCode = errorCode;
        this.args = args;

        log.debug("BusinessException created: code={}, args={}", errorCode, args);
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
    private static String format(ErrorCode errorCode, Object[] args) {
        String msg = errorCode.getDefaultMessage();

        if (args == null || args.length == 0) {
            return msg;
        }

        try {
            return String.format(msg, args);
        } catch (Exception ex) {
            log.warn("Message formatting failed for code={}", errorCode, ex);
            return msg;
        }
    }

}
