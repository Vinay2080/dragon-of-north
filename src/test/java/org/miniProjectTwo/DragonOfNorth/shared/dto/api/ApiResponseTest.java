package org.miniProjectTwo.DragonOfNorth.shared.dto.api;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.shared.dto.api.ApiResponse;
import org.miniProjectTwo.DragonOfNorth.shared.enums.ApiResponseStatus;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void success_withData_shouldCreateSuccessResponse() {
        // arrange
        String testData = "test data";

        // act
        ApiResponse<String> response = ApiResponse.success(testData);

        // assert
        assertNotNull(response);
        assertNull(response.getMessage());
        assertEquals(ApiResponseStatus.success, response.getApiResponseStatus());
        assertEquals(testData, response.getData());
        assertNotNull(response.getTime());
        assertTrue(response.getTime().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void success_withNullData_shouldCreateSuccessResponse() {
        // act
        ApiResponse<String> response = ApiResponse.success(null);

        // assert
        assertNotNull(response);
        assertNull(response.getMessage());
        assertEquals(ApiResponseStatus.success, response.getApiResponseStatus());
        assertNull(response.getData());
        assertNotNull(response.getTime());
    }

    @Test
    void successMessage_shouldCreateSuccessResponseWithMessage() {
        // arrange
        String message = "Operation completed successfully";

        // act
        ApiResponse<?> response = ApiResponse.successMessage(message);

        // assert
        assertNotNull(response);
        assertEquals(message, response.getMessage());
        assertEquals(ApiResponseStatus.success, response.getApiResponseStatus());
        assertNull(response.getData());
        assertNotNull(response.getTime());
    }

    @Test
    void successMessage_withNullMessage_shouldCreateSuccessResponse() {
        // act
        ApiResponse<?> response = ApiResponse.successMessage(null);

        // assert
        assertNotNull(response);
        assertNull(response.getMessage());
        assertEquals(ApiResponseStatus.success, response.getApiResponseStatus());
        assertNull(response.getData());
        assertNotNull(response.getTime());
    }

    @Test
    void failed_withData_shouldCreateFailedResponse() {
        // arrange
        String errorData = "Error details";

        // act
        ApiResponse<String> response = ApiResponse.failed(errorData);

        // assert
        assertNotNull(response);
        assertNull(response.getMessage());
        assertEquals(ApiResponseStatus.failed, response.getApiResponseStatus());
        assertEquals(errorData, response.getData());
        assertNotNull(response.getTime());
    }

    @Test
    void failed_withNullData_shouldCreateFailedResponse() {
        // act
        ApiResponse<String> response = ApiResponse.failed(null);

        // assert
        assertNotNull(response);
        assertNull(response.getMessage());
        assertEquals(ApiResponseStatus.failed, response.getApiResponseStatus());
        assertNull(response.getData());
        assertNotNull(response.getTime());
    }

    @Test
    void time_shouldBeSetToCurrentTime() {
        // arrange
        Instant beforeCreation = Instant.now();

        // act
        ApiResponse<String> response = ApiResponse.success("test");

        // assert
        Instant afterCreation = Instant.now();
        assertNotNull(response.getTime());
        assertTrue(response.getTime().isAfter(beforeCreation) || response.getTime().equals(beforeCreation));
        assertTrue(response.getTime().isBefore(afterCreation) || response.getTime().equals(afterCreation));
    }

    @Test
    void responseWithComplexData_shouldWork() {
        // arrange
        TestData complexData = new TestData("test", 123);

        // act
        ApiResponse<TestData> response = ApiResponse.success(complexData);

        // assert
        assertNotNull(response);
        assertEquals(complexData, response.getData());
        assertEquals(ApiResponseStatus.success, response.getApiResponseStatus());
    }

    @Test
    void responseWithListData_shouldWork() {
        // arrange
        var listData = java.util.List.of("item1", "item2", "item3");

        // act
        ApiResponse<java.util.List<String>> response = ApiResponse.success(listData);

        // assert
        assertNotNull(response);
        assertEquals(listData, response.getData());
        assertEquals(3, response.getData().size());
        assertEquals(ApiResponseStatus.success, response.getApiResponseStatus());
    }

    @Test
    void genericTypeInference_shouldWork() {
        // act
        ApiResponse<String> stringResponse = ApiResponse.success("string");
        ApiResponse<Integer> intResponse = ApiResponse.success(42);
        ApiResponse<Boolean> boolResponse = ApiResponse.success(true);

        // assert
        assertEquals("string", stringResponse.getData());
        assertEquals(Integer.valueOf(42), intResponse.getData());
        assertEquals(Boolean.TRUE, boolResponse.getData());
    }

    // Helper class for testing complex data
    private record TestData(String name, int value) {

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestData testData = (TestData) obj;
            return value == testData.value && name.equals(testData.name);
        }

    }
}
