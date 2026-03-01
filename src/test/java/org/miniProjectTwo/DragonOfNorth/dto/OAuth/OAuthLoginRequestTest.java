package org.miniProjectTwo.DragonOfNorth.dto.OAuth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuthLoginRequestTest {

    @Test
    void builder_shouldCreateRequestWithAllFields() {
        // act
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .idToken("sample-id-token-123456789")
                .deviceId("device-123")
                .expectedIdentifier("test@example.com")
                .build();

        // assert
        assertEquals("sample-id-token-123456789", request.idToken());
        assertEquals("device-123", request.deviceId());
        assertEquals("test@example.com", request.expectedIdentifier());
    }

    @Test
    void record_shouldWorkCorrectly() {
        // arrange
        OAuthLoginRequest request = new OAuthLoginRequest("test-token", "test-device", "test@example.com");

        // assert
        assertEquals("test-token", request.idToken());
        assertEquals("test-device", request.deviceId());
        assertEquals("test@example.com", request.expectedIdentifier());
    }

    @Test
    void equals_shouldWorkCorrectly() {
        // arrange
        OAuthLoginRequest request1 = new OAuthLoginRequest("token", "device", "email@example.com");
        OAuthLoginRequest request2 = new OAuthLoginRequest("token", "device", "email@example.com");
        OAuthLoginRequest request3 = new OAuthLoginRequest("different-token", "device", "email@example.com");

        // assert
        assertEquals(request1, request2);
        assertNotEquals(request1, request3);
    }

    @Test
    void hashCode_shouldWorkCorrectly() {
        // arrange
        OAuthLoginRequest request1 = new OAuthLoginRequest("token", "device", "email@example.com");
        OAuthLoginRequest request2 = new OAuthLoginRequest("token", "device", "email@example.com");

        // assert
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void toString_shouldContainAllFields() {
        // arrange
        OAuthLoginRequest request = new OAuthLoginRequest("test-token", "test-device", "test@example.com");

        // act
        String result = request.toString();

        // assert
        assertTrue(result.contains("test-token"));
        assertTrue(result.contains("test-device"));
        assertTrue(result.contains("test@example.com"));
    }

    @Test
    void builder_shouldCreateRequestWithNullExpectedIdentifier() {
        // act
        OAuthLoginRequest request = OAuthLoginRequest.builder()
                .idToken("sample-id-token-123456789")
                .deviceId("device-123")
                .build();

        // assert
        assertEquals("sample-id-token-123456789", request.idToken());
        assertEquals("device-123", request.deviceId());
        assertNull(request.expectedIdentifier());
    }
}
