package org.miniProjectTwo.DragonOfNorth.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ExceptionHandlerFilterTest {

    @Test
    void doFilterInternal_shouldWriteApiResponse_whenBusinessExceptionThrown() throws Exception {
        // arrange
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ExceptionHandlerFilter filter = new ExceptionHandlerFilter(objectMapper);

        FilterChain chain = mock(FilterChain.class);
        doThrow(new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED)).when(chain)
                .doFilter(any(), any());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // act
        filter.doFilterInternal(request, response, chain);

        // assert
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus().value(), response.getStatus());
        assertEquals("application/json", response.getContentType());

        JsonNode json = objectMapper.readTree(response.getContentAsString());
        assertEquals("failed", json.get("apiResponseStatus").asText());
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED.getCode(), json.get("data").get("code").asText());
    }

    @Test
    void doFilterInternal_shouldDelegate_whenNoException() throws Exception {
        // arrange
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ExceptionHandlerFilter filter = new ExceptionHandlerFilter(objectMapper);

        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // act
        filter.doFilterInternal(request, response, chain);

        // assert
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("", response.getContentAsString());
    }
}
