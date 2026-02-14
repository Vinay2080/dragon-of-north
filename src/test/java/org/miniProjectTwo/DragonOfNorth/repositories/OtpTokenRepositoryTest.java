package org.miniProjectTwo.DragonOfNorth.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.model.OtpToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpTokenRepositoryTest {

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Test
    void findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc_shouldReturnToken_whenExists() {
        // arrange
        String identifier = "test@example.com";
        IdentifierType type = IdentifierType.EMAIL;
        OtpPurpose purpose = OtpPurpose.SIGNUP;
        OtpToken expectedToken = createTestOtpToken();

        when(otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
                identifier, type, purpose)).thenReturn(Optional.of(expectedToken));

        // act
        Optional<OtpToken> result = otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
                identifier, type, purpose);

        // assert
        assertTrue(result.isPresent());
        assertEquals(expectedToken, result.get());
        verify(otpTokenRepository).findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
                identifier, type, purpose);
    }

    private OtpToken createTestOtpToken() {
        OtpToken token = new OtpToken();
        token.setId(1L);
        token.setIdentifier("test@example.com");
        token.setType(IdentifierType.EMAIL);
        token.setOtpPurpose(OtpPurpose.SIGNUP);
        token.setOtpHash("hashed-123456");
        token.setCreatedAt(Instant.now());
        token.setLastSentAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(300));
        token.setAttempts(0);
        token.setConsumed(false);
        return token;
    }

    @Test
    void findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc_shouldReturnEmpty_whenNotExists() {
        // arrange
        String identifier = "nonexistent@example.com";
        IdentifierType type = IdentifierType.EMAIL;
        OtpPurpose purpose = OtpPurpose.SIGNUP;

        when(otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
                identifier, type, purpose)).thenReturn(Optional.empty());

        // act
        Optional<OtpToken> result = otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
                identifier, type, purpose);

        // assert
        assertFalse(result.isPresent());
        verify(otpTokenRepository).findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
                identifier, type, purpose);
    }

    @Test
    void countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter_shouldReturnCount() {
        // arrange
        String identifier = "test@example.com";
        IdentifierType type = IdentifierType.EMAIL;
        OtpPurpose purpose = OtpPurpose.SIGNUP;
        Instant after = Instant.now().minusSeconds(3600);
        int expectedCount = 3;

        when(otpTokenRepository.countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter(
                identifier, type, purpose, after)).thenReturn(expectedCount);

        // act
        int result = otpTokenRepository.countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter(
                identifier, type, purpose, after);

        // assert
        assertEquals(expectedCount, result);
        verify(otpTokenRepository).countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter(
                identifier, type, purpose, after);
    }

    @Test
    void countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter_shouldReturnZero_whenNoTokens() {
        // arrange
        String identifier = "test@example.com";
        IdentifierType type = IdentifierType.EMAIL;
        OtpPurpose purpose = OtpPurpose.SIGNUP;
        Instant after = Instant.now();

        when(otpTokenRepository.countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter(
                identifier, type, purpose, after)).thenReturn(0);

        // act
        int result = otpTokenRepository.countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter(
                identifier, type, purpose, after);

        // assert
        assertEquals(0, result);
        verify(otpTokenRepository).countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter(
                identifier, type, purpose, after);
    }

    @Test
    void deleteAllByExpiresAtBefore_shouldCallDeleteMethod() {
        // arrange
        Instant cutoff = Instant.now();

        // act
        otpTokenRepository.deleteAllByExpiresAtBefore(cutoff);

        // assert
        verify(otpTokenRepository).deleteAllByExpiresAtBefore(cutoff);
    }

    @Test
    void findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc_shouldWorkForPhone() {
        // arrange
        String identifier = "+1234567890";
        IdentifierType type = IdentifierType.PHONE;
        OtpPurpose purpose = OtpPurpose.LOGIN;
        OtpToken expectedToken = createTestOtpToken();

        when(otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
                identifier, type, purpose)).thenReturn(Optional.of(expectedToken));

        // act
        Optional<OtpToken> result = otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
                identifier, type, purpose);

        // assert
        assertTrue(result.isPresent());
        assertEquals(expectedToken, result.get());
        verify(otpTokenRepository).findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
                identifier, type, purpose);
    }
}
