package org.miniProjectTwo.DragonOfNorth.security.filter;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SidSensitiveRouteValidatorTest {

    @Test
    void validatePatterns_shouldPassWhenPatternsMatchMappedRoutes() throws NoSuchMethodException {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        RequestMappingInfo sessions = RequestMappingInfo.paths("/api/v1/sessions/get/all").methods(RequestMethod.GET).build();
        RequestMappingInfo passwordChange = RequestMappingInfo.paths("/api/v1/auth/password/change").methods(RequestMethod.POST).build();
        HandlerMethod handlerMethod = new HandlerMethod(new DummyController(), DummyController.class.getMethod("handler"));
        when(mapping.getHandlerMethods()).thenReturn(Map.of(sessions, handlerMethod, passwordChange, handlerMethod));

        SidSensitiveRouteValidator validator = new SidSensitiveRouteValidator(
                "sensitive-only",
                "/api/v1/sessions/**,/api/v1/auth/password/change",
                mapping
        );

        assertDoesNotThrow(validator::validatePatterns);
    }

    @Test
    void validatePatterns_shouldFailClosedWhenPatternIsStale() throws NoSuchMethodException {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        RequestMappingInfo sessions = RequestMappingInfo.paths("/api/v1/sessions/get/all").methods(RequestMethod.GET).build();
        HandlerMethod handlerMethod = new HandlerMethod(new DummyController(), DummyController.class.getMethod("handler"));
        when(mapping.getHandlerMethods()).thenReturn(Map.of(sessions, handlerMethod));

        SidSensitiveRouteValidator validator = new SidSensitiveRouteValidator(
                "sensitive-only",
                "/api/v1/session/**",
                mapping
        );

        assertThrows(IllegalStateException.class, validator::validatePatterns);
    }

    static class DummyController {
        public void handler() {}
    }
}
