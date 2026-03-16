package org.miniProjectTwo.DragonOfNorth.shared.dto.oauth;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.shared.dto.oauth.OAuthUserInfo;

import static org.junit.jupiter.api.Assertions.*;

class OAuthUserInfoTest {

    @Test
    void builder_shouldCreateInfoWithAllFields() {
        // act
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .sub("123456789")
                .email("test@example.com")
                .emailVerified(true)
                .name("Test User")
                .picture("https://example.com/picture.jpg")
                .issuer("https://accounts.google.com")
                .audience("your-client-id")
                .expirationTime(1234567890L)
                .issuedAtTime(1234567800L)
                .build();

        // assert
        assertEquals("123456789", userInfo.sub());
        assertEquals("test@example.com", userInfo.email());
        assertTrue(userInfo.emailVerified());
        assertEquals("Test User", userInfo.name());
        assertEquals("https://example.com/picture.jpg", userInfo.picture());
        assertEquals("https://accounts.google.com", userInfo.issuer());
        assertEquals("your-client-id", userInfo.audience());
        assertEquals(1234567890L, userInfo.expirationTime());
        assertEquals(1234567800L, userInfo.issuedAtTime());
    }

    @Test
    void record_shouldWorkCorrectly() {
        // arrange
        OAuthUserInfo userInfo = new OAuthUserInfo(
                "123456789",
                "test@example.com",
                true,
                "Test User",
                "https://example.com/picture.jpg",
                "https://accounts.google.com",
                "your-client-id",
                1234567890L,
                1234567800L
        );

        // assert
        assertEquals("123456789", userInfo.sub());
        assertEquals("test@example.com", userInfo.email());
        assertTrue(userInfo.emailVerified());
        assertEquals("Test User", userInfo.name());
        assertEquals("https://example.com/picture.jpg", userInfo.picture());
        assertEquals("https://accounts.google.com", userInfo.issuer());
        assertEquals("your-client-id", userInfo.audience());
        assertEquals(1234567890L, userInfo.expirationTime());
        assertEquals(1234567800L, userInfo.issuedAtTime());
    }

    @Test
    void equals_shouldWorkCorrectly() {
        // arrange
        OAuthUserInfo userInfo1 = new OAuthUserInfo(
                "123", "email@example.com", true, "Name", "pic", "issuer", "audience", 1L, 2L
        );
        OAuthUserInfo userInfo2 = new OAuthUserInfo(
                "123", "email@example.com", true, "Name", "pic", "issuer", "audience", 1L, 2L
        );
        OAuthUserInfo userInfo3 = new OAuthUserInfo(
                "456", "email@example.com", true, "Name", "pic", "issuer", "audience", 1L, 2L
        );

        // assert
        assertEquals(userInfo1, userInfo2);
        assertNotEquals(userInfo1, userInfo3);
    }

    @Test
    void hashCode_shouldWorkCorrectly() {
        // arrange
        OAuthUserInfo userInfo1 = new OAuthUserInfo(
                "123", "email@example.com", true, "Name", "pic", "issuer", "audience", 1L, 2L
        );
        OAuthUserInfo userInfo2 = new OAuthUserInfo(
                "123", "email@example.com", true, "Name", "pic", "issuer", "audience", 1L, 2L
        );

        // assert
        assertEquals(userInfo1.hashCode(), userInfo2.hashCode());
    }

    @Test
    void toString_shouldContainAllFields() {
        // arrange
        OAuthUserInfo userInfo = new OAuthUserInfo(
                "123", "test@example.com", true, "Test", "pic", "issuer", "audience", 1L, 2L
        );

        // act
        String result = userInfo.toString();

        // assert
        assertTrue(result.contains("123"));
        assertTrue(result.contains("test@example.com"));
        assertTrue(result.contains("Test"));
    }

    @Test
    void builder_shouldCreateWithNullValues() {
        // act
        OAuthUserInfo userInfo = OAuthUserInfo.builder()
                .sub("123")
                .email("test@example.com")
                .build();

        // assert
        assertEquals("123", userInfo.sub());
        assertEquals("test@example.com", userInfo.email());
        assertFalse(userInfo.emailVerified()); // primitive boolean defaults to false
        assertNull(userInfo.name());
        assertNull(userInfo.picture());
        assertNull(userInfo.issuer());
        assertNull(userInfo.audience());
        assertNull(userInfo.expirationTime());
        assertNull(userInfo.issuedAtTime());
    }
}
