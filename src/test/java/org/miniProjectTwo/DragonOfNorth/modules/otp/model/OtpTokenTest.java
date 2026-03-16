package org.miniProjectTwo.DragonOfNorth.modules.otp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.modules.otp.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpPurpose;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class OtpTokenTest {

    private static final String IDENTIFIER = "test@example.com";
    private static final String OTP_HASH = "$2a$10$hashedPassword";
    private static final int TTL_MINUTES = 5;
    private OtpToken otpToken;

    @BeforeEach
    void setUp() {
        otpToken = new OtpToken();
    }

    @Test
    void constructor_shouldCreateEmptyOtpToken() {
        // assert
        assertNotNull(otpToken);
        assertEquals(0, otpToken.getId());
        assertNull(otpToken.getIdentifier());
        assertNull(otpToken.getType());
        assertNull(otpToken.getOtpPurpose());
        assertNull(otpToken.getOtpHash());
        assertNull(otpToken.getCreatedAt());
        assertNull(otpToken.getLastSentAt());
        assertNull(otpToken.getExpiresAt());
        assertEquals(0, otpToken.getAttempts());
        assertFalse(otpToken.isConsumed());
        assertNull(otpToken.getRequestIp());
        assertNull(otpToken.getVerifiedAt());
        assertNull(otpToken.getVersion());
    }

    @Test
    void constructorWithParameters_shouldCreateOtpToken() {
        // act
        OtpToken token = new OtpToken(IDENTIFIER, IdentifierType.EMAIL, OTP_HASH, TTL_MINUTES, OtpPurpose.SIGNUP);

        // assert
        assertEquals(IDENTIFIER, token.getIdentifier());
        assertEquals(IdentifierType.EMAIL, token.getType());
        assertEquals(OTP_HASH, token.getOtpHash());
        assertEquals(OtpPurpose.SIGNUP, token.getOtpPurpose());
        assertNotNull(token.getCreatedAt());
        assertNotNull(token.getLastSentAt());
        assertEquals(token.getCreatedAt(), token.getLastSentAt());
        assertNotNull(token.getExpiresAt());
        assertTrue(token.getExpiresAt().isAfter(token.getCreatedAt()));
        assertEquals(0, token.getAttempts());
        assertFalse(token.isConsumed());
        assertNull(token.getRequestIp());
        assertNull(token.getVerifiedAt());
    }

    @Test
    void constructorWithParameters_shouldCalculateExpirationCorrectly() {
        // act
        OtpToken token = new OtpToken(IDENTIFIER, IdentifierType.PHONE, OTP_HASH, TTL_MINUTES, OtpPurpose.LOGIN);
        Instant expectedExpiration = token.getCreatedAt().plusSeconds(TTL_MINUTES * 60L);

        // assert
        assertEquals(expectedExpiration, token.getExpiresAt());
    }

    @Test
    void isExpired_shouldReturnFalse_whenTokenNotExpired() {
        // arrange
        OtpToken token = new OtpToken(IDENTIFIER, IdentifierType.EMAIL, OTP_HASH, TTL_MINUTES, OtpPurpose.SIGNUP);

        // act & assert
        assertFalse(token.isExpired());
    }

    @Test
    void isExpired_shouldReturnTrue_whenTokenExpired() {
        // arrange
        OtpToken token = new OtpToken(IDENTIFIER, IdentifierType.EMAIL, OTP_HASH, 0, OtpPurpose.SIGNUP);
        // Wait a moment to ensure expiration
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // act & assert
        assertTrue(token.isExpired());
    }

    @Test
    void incrementAttempts_shouldIncreaseAttemptsByOne() {
        // arrange
        OtpToken token = new OtpToken(IDENTIFIER, IdentifierType.EMAIL, OTP_HASH, TTL_MINUTES, OtpPurpose.SIGNUP);
        int initialAttempts = token.getAttempts();

        // act
        token.incrementAttempts();

        // assert
        assertEquals(initialAttempts + 1, token.getAttempts());
    }

    @Test
    void incrementAttempts_shouldWorkMultipleTimes() {
        // arrange
        OtpToken token = new OtpToken(IDENTIFIER, IdentifierType.EMAIL, OTP_HASH, TTL_MINUTES, OtpPurpose.SIGNUP);

        // act
        token.incrementAttempts();
        token.incrementAttempts();
        token.incrementAttempts();

        // assert
        assertEquals(3, token.getAttempts());
    }

    @Test
    void markVerified_shouldSetConsumedToTrue() {
        // arrange
        OtpToken token = new OtpToken(IDENTIFIER, IdentifierType.EMAIL, OTP_HASH, TTL_MINUTES, OtpPurpose.SIGNUP);

        // act
        token.markVerified();

        // assert
        assertTrue(token.isConsumed());
    }

    @Test
    void markVerified_shouldSetVerifiedAt() {
        // arrange
        OtpToken token = new OtpToken(IDENTIFIER, IdentifierType.EMAIL, OTP_HASH, TTL_MINUTES, OtpPurpose.SIGNUP);
        Instant beforeVerification = Instant.now();

        // act
        token.markVerified();

        // assert
        assertNotNull(token.getVerifiedAt());
        assertTrue(token.getVerifiedAt().isAfter(beforeVerification) ||
                token.getVerifiedAt().equals(beforeVerification));
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // arrange
        Instant now = Instant.now();

        // act
        otpToken.setId(123L);
        otpToken.setIdentifier(IDENTIFIER);
        otpToken.setType(IdentifierType.PHONE);
        otpToken.setOtpPurpose(OtpPurpose.LOGIN);
        otpToken.setOtpHash(OTP_HASH);
        otpToken.setCreatedAt(now);
        otpToken.setLastSentAt(now.plusSeconds(10));
        otpToken.setExpiresAt(now.plusSeconds(300));
        otpToken.setAttempts(5);
        otpToken.setConsumed(true);
        otpToken.setRequestIp("192.168.1.1");
        otpToken.setVerifiedAt(now.plusSeconds(20));
        otpToken.setVersion(2L);

        // assert
        assertEquals(123L, otpToken.getId());
        assertEquals(IDENTIFIER, otpToken.getIdentifier());
        assertEquals(IdentifierType.PHONE, otpToken.getType());
        assertEquals(OtpPurpose.LOGIN, otpToken.getOtpPurpose());
        assertEquals(OTP_HASH, otpToken.getOtpHash());
        assertEquals(now, otpToken.getCreatedAt());
        assertEquals(now.plusSeconds(10), otpToken.getLastSentAt());
        assertEquals(now.plusSeconds(300), otpToken.getExpiresAt());
        assertEquals(5, otpToken.getAttempts());
        assertTrue(otpToken.isConsumed());
        assertEquals("192.168.1.1", otpToken.getRequestIp());
        assertEquals(now.plusSeconds(20), otpToken.getVerifiedAt());
        assertEquals(2L, otpToken.getVersion());
    }

    @Test
    void builder_shouldCreateOtpToken() {
        // act
        OtpToken token = OtpToken.builder()
                .identifier(IDENTIFIER)
                .type(IdentifierType.EMAIL)
                .otpPurpose(OtpPurpose.SIGNUP)
                .otpHash(OTP_HASH)
                .attempts(2)
                .consumed(true)
                .requestIp("127.0.0.1")
                .build();

        // assert
        assertEquals(IDENTIFIER, token.getIdentifier());
        assertEquals(IdentifierType.EMAIL, token.getType());
        assertEquals(OtpPurpose.SIGNUP, token.getOtpPurpose());
        assertEquals(OTP_HASH, token.getOtpHash());
        assertEquals(2, token.getAttempts());
        assertTrue(token.isConsumed());
        assertEquals("127.0.0.1", token.getRequestIp());
    }
}
