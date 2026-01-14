package org.miniProjectTwo.DragonOfNorth.impl.otp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.enums.OtpPurpose;
import org.miniProjectTwo.DragonOfNorth.enums.OtpVerificationStatus;
import org.miniProjectTwo.DragonOfNorth.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.repositories.OtpTokenRepository;
import org.miniProjectTwo.DragonOfNorth.services.OtpSender;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @InjectMocks
    private OtpService otpService;

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private OtpSender emailOtpSender;

    @Mock
    private OtpSender phoneOtpSender;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpTokenRepository, emailOtpSender, phoneOtpSender);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "ttlMinutes", 5);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "requestWindowSeconds", 3600);
        ReflectionTestUtils.setField(otpService, "blockDurationMinutes", 15);
        ReflectionTestUtils.setField(otpService, "resendCooldownSeconds", 60);
        ReflectionTestUtils.setField(otpService, "maxRequestsPerWindow", 5);
    }

    @Test
    void createPhoneOtp_shouldCreateAndSendOtp_whenRateLimitsAreNotExceeded() {
        // arrange
        String phone = "1234567890";
        OtpPurpose purpose = OtpPurpose.SIGNUP;
        when(otpTokenRepository.
                findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc
                        (anyString(), any(IdentifierType.class), any(OtpPurpose.class)))
                .thenReturn(Optional.empty());
        when(otpTokenRepository.
                countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter
                        (anyString(), any(IdentifierType.class), any(OtpPurpose.class), any(Instant.class)))
                .thenReturn(0);

        // act
        otpService.createPhoneOtp(phone, purpose);

        // verify
        verify(otpTokenRepository).save(any(OtpToken.class));
        verify(phoneOtpSender, atLeastOnce()).send(anyString(), anyString(), anyInt());
    }

    @Test
    void createEmailOtp_shouldThrowException_whenCooldownPeriodActive() {
        // arrange
        String email = "test@example.com";
        OtpPurpose purpose = OtpPurpose.SIGNUP;
        OtpToken lastToken = new OtpToken(email, IdentifierType.EMAIL, "hash", 5, purpose);
        lastToken.setLastSentAt(Instant.now().minusSeconds(30)); // The 30s ago, cooldown was 60s

        when(otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(email, IdentifierType.EMAIL, purpose))
                .thenReturn(Optional.of(lastToken));

        // act & assert
        assertThrows(IllegalStateException.class, () -> otpService.createEmailOtp(email, purpose));

        verify(emailOtpSender, never()).send(any(), any(), anyInt());
    }

    @Test
    void verifyEmailOtp_shouldReturnSuccess_whenOtpIsCorrect() {
        // arrange
        String email = "test@example.com";
        String otp = "123456";
        OtpPurpose purpose = OtpPurpose.SIGNUP;
        String hash = BCrypt.hashpw(otp, BCrypt.gensalt());
        OtpToken token = new OtpToken(email, IdentifierType.EMAIL, hash, 5, purpose);

        when(otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(email, IdentifierType.EMAIL, purpose))
                .thenReturn(Optional.of(token));

        // act
        OtpVerificationStatus status = otpService.verifyEmailOtp(email, otp, purpose);

        // assert
        assertEquals(OtpVerificationStatus.SUCCESS, status);
        assertTrue(token.isConsumed());
        assertNotNull(token.getVerifiedAt());
        verify(otpTokenRepository).save(token);
    }

    @Test
    void verifyEmailOtp_shouldReturnInvalidOtp_whenOtpIsIncorrect() {
        // arrange
        String email = "test@example.com";
        String otp = "123456";
        String providedOtp = "654321";
        OtpPurpose purpose = OtpPurpose.SIGNUP;
        String hash = BCrypt.hashpw(otp, BCrypt.gensalt());
        OtpToken token = new OtpToken(email, IdentifierType.EMAIL, hash, 5, purpose);

        when(otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(email, IdentifierType.EMAIL, purpose))
                .thenReturn(Optional.of(token));

        // act
        OtpVerificationStatus status = otpService.verifyEmailOtp(email, providedOtp, purpose);

        // assert
        assertEquals(OtpVerificationStatus.INVALID_OTP, status);
        assertFalse(token.isConsumed());
        assertEquals(1, token.getAttempts());
        verify(otpTokenRepository).save(token);
    }

    @Test
    void verifyEmailOtp_shouldReturnExpiredOtp_whenTokenIsExpired() {
        // arrange
        String email = "test@example.com";
        OtpPurpose purpose = OtpPurpose.SIGNUP;
        OtpToken token = new OtpToken(email, IdentifierType.EMAIL, "hash", 5, purpose);
        token.setExpiresAt(Instant.now().minusSeconds(10));

        when(otpTokenRepository.findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(email, IdentifierType.EMAIL, purpose))
                .thenReturn(Optional.of(token));

        // act
        OtpVerificationStatus status = otpService.verifyEmailOtp(email, "123456", purpose);

        // assert
        assertEquals(OtpVerificationStatus.EXPIRED_OTP, status);
    }
}
