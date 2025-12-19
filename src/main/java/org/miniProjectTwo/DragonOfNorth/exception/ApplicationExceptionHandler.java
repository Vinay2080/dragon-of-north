package org.miniProjectTwo.DragonOfNorth.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.miniProjectTwo.DragonOfNorth.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.dto.api.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

import static org.miniProjectTwo.DragonOfNorth.dto.api.ErrorResponse.ValidationError;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ApplicationExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(BusinessException businessException) {
        // Builds error response from a business exception
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .code(businessException.getErrorCode().getCode())
                .defaultMessage(businessException.getErrorCode().getDefaultMessage())
                .build();

        return ResponseEntity
                .status(businessException.getErrorCode().getHttpStatus())
                .body(ApiResponse.failed(errorResponse));

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(MethodArgumentNotValidException exception) {
        final List<ValidationError> list = new ArrayList<>();
        // Extracts validation errors into a structured list
        exception
                .getBindingResult()
                .getAllErrors()
                .forEach(
                        errors -> {
                            final String fieldName = ((FieldError) errors).getField();
                            final String errorCode = errors.getDefaultMessage();
                            list.add(ValidationError
                                    .builder()
                                    .field(fieldName)
                                    .code(errorCode)
                                    .message(errorCode)
                                    .build()
                            );
                        }
                );
        final ErrorResponse errorResponse = ErrorResponse.builder()
                .validationErrorList(list)
                .build();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failed(errorResponse));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(BadCredentialsException exception){
//       ErrorResponse errorResponse = ErrorResponse
//               .builder()
//               .code().defaultMessage().validationErrorList().build()
        return null;
    }
    //todo do it later. Enough for now.
}
