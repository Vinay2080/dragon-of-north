package org.miniProjectTwo.DragonOfNorth.shared.dto.api;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ErrorResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void constructor_shouldCreateErrorResponse() {
        // arrange
        String code = "ERROR_001";
        String defaultMessage = "An error occurred";
        List<ErrorResponse.ValidationError> validationErrors = List.of(
                new ErrorResponse.ValidationError("field1", "VALIDATION_ERROR", "Field is required")
        );

        // act
        ErrorResponse errorResponse = new ErrorResponse(code, defaultMessage, validationErrors);

        // assert
        assertEquals(code, errorResponse.code());
        assertEquals(defaultMessage, errorResponse.defaultMessage());
        assertEquals(validationErrors, errorResponse.validationErrorList());
    }

    @Test
    void constructor_withNullValidationErrors_shouldCreateErrorResponse() {
        // arrange
        String code = "ERROR_002";
        String defaultMessage = "Another error";

        // act
        ErrorResponse errorResponse = new ErrorResponse(code, defaultMessage, null);

        // assert
        assertEquals(code, errorResponse.code());
        assertEquals(defaultMessage, errorResponse.defaultMessage());
        assertNull(errorResponse.validationErrorList());
    }

    @Test
    void constructor_withEmptyValidationErrors_shouldCreateErrorResponse() {
        // arrange
        String code = "ERROR_003";
        String defaultMessage = "Validation errors";
        List<ErrorResponse.ValidationError> validationErrors = List.of();

        // act
        ErrorResponse errorResponse = new ErrorResponse(code, defaultMessage, validationErrors);

        // assert
        assertEquals(code, errorResponse.code());
        assertEquals(defaultMessage, errorResponse.defaultMessage());
        assertTrue(errorResponse.validationErrorList().isEmpty());
    }

    @Test
    void validationErrorConstructor_shouldCreateValidationError() {
        // arrange
        String field = "email";
        String errorCode = "INVALID_EMAIL";
        String message = "Email format is invalid";

        // act
        ErrorResponse.ValidationError validationError = new ErrorResponse.ValidationError(field, errorCode, message);

        // assert
        assertEquals(field, validationError.getField());
        assertEquals(errorCode, validationError.getCode());
        assertEquals(message, validationError.getMessage());
    }

    @Test
    void validationErrorSettersAndGetters_shouldWork() {
        // arrange
        ErrorResponse.ValidationError validationError = new ErrorResponse.ValidationError();

        // act
        validationError.setField("username");
        validationError.setCode("USERNAME_REQUIRED");
        validationError.setMessage("Username is required");

        // assert
        assertEquals("username", validationError.getField());
        assertEquals("USERNAME_REQUIRED", validationError.getCode());
        assertEquals("Username is required", validationError.getMessage());
    }

    @Test
    void validationErrorBuilder_shouldCreateValidationError() {
        // act
        ErrorResponse.ValidationError validationError = ErrorResponse.ValidationError.builder()
                .field("password")
                .code("PASSWORD_TOO_SHORT")
                .message("Password must be at least 8 characters")
                .build();

        // assert
        assertEquals("password", validationError.getField());
        assertEquals("PASSWORD_TOO_SHORT", validationError.getCode());
        assertEquals("Password must be at least 8 characters", validationError.getMessage());
    }

    @Test
    void validationErrorAllArgsConstructor_shouldCreateValidationError() {
        // act
        ErrorResponse.ValidationError validationError = new ErrorResponse.ValidationError(
                "phone",
                "INVALID_PHONE",
                "Phone number format is invalid"
        );

        // assert
        assertEquals("phone", validationError.getField());
        assertEquals("INVALID_PHONE", validationError.getCode());
        assertEquals("Phone number format is invalid", validationError.getMessage());
    }

    @Test
    void validationErrorNoArgsConstructor_shouldCreateEmptyValidationError() {
        // act
        ErrorResponse.ValidationError validationError = new ErrorResponse.ValidationError();

        // assert
        assertNull(validationError.getField());
        assertNull(validationError.getCode());
        assertNull(validationError.getMessage());
    }

    @Test
    void validationErrorEquals_shouldUseDefaultObjectEquals() {
        // arrange
        ErrorResponse.ValidationError error1 = new ErrorResponse.ValidationError("field", "CODE", "message");
        ErrorResponse.ValidationError error2 = new ErrorResponse.ValidationError("field", "CODE", "message");

        // assert - Using default Object.equals, different instances are not equal
        assertNotEquals(error1, error2);
    }

    @Test
    void validationError_shouldHaveWorkingToString() {
        // arrange
        ErrorResponse.ValidationError validationError = new ErrorResponse.ValidationError("testField", "TEST_CODE", "Test message");

        // act
        String toString = validationError.toString();

        // assert - Just verify toString doesn't throw an exception and returns something
        assertNotNull(toString);
        assertFalse(toString.isEmpty());
    }

    @Test
    void errorResponseWithMultipleValidationErrors_shouldWork() {
        // arrange
        ErrorResponse errorResponse = getErrorResponse();

        // assert
        assertEquals("VALIDATION_FAILED", errorResponse.code());
        assertEquals("Multiple validation errors", errorResponse.defaultMessage());
        assertEquals(3, errorResponse.validationErrorList().size());
        assertTrue(errorResponse.validationErrorList().stream()
                .anyMatch(error -> "email".equals(error.getField())));
        assertTrue(errorResponse.validationErrorList().stream()
                .anyMatch(error -> "password".equals(error.getField())));
        assertTrue(errorResponse.validationErrorList().stream()
                .anyMatch(error -> "name".equals(error.getField())));
    }

    private static @NonNull ErrorResponse getErrorResponse() {
        List<ErrorResponse.ValidationError> validationErrors = List.of(
                new ErrorResponse.ValidationError("email", "INVALID_EMAIL", "Email is invalid"),
                new ErrorResponse.ValidationError("password", "PASSWORD_TOO_SHORT", "Password too short"),
                new ErrorResponse.ValidationError("name", "NAME_REQUIRED", "Name is required")
        );

        // act
        return new ErrorResponse("VALIDATION_FAILED", "Multiple validation errors", validationErrors);
    }

    @Test
    void errorResponseRecordMethods_shouldWork() {
        // arrange
        String code = "TEST_ERROR";
        String message = "Test error message";
        List<ErrorResponse.ValidationError> errors = List.of(
                new ErrorResponse.ValidationError("field", "CODE", "message")
        );

        // act
        ErrorResponse errorResponse = new ErrorResponse(code, message, errors);

        // assert
        assertEquals(code, errorResponse.code());
        assertEquals(message, errorResponse.defaultMessage());
        assertEquals(errors, errorResponse.validationErrorList());

        // Test record equals
        ErrorResponse sameResponse = new ErrorResponse(code, message, errors);
        assertEquals(errorResponse, sameResponse);

        // Test record hashCode
        assertEquals(errorResponse.hashCode(), sameResponse.hashCode());
    }
}
