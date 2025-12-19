package org.miniProjectTwo.DragonOfNorth.dto.api;


import lombok.*;

import java.util.List;

@Builder
public record ErrorResponse(
        String code,
        String defaultMessage,
        List<ValidationError> validationErrorList
) {
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        private String field;
        private String code;
        private String message;
    }
}
