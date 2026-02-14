package org.miniProjectTwo.DragonOfNorth.serviceInterfaces;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.components.TokenHasher;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.RefreshToken;
import org.miniProjectTwo.DragonOfNorth.repositories.RefreshTokenRepository;
import org.miniProjectTwo.DragonOfNorth.services.auth.RefreshTokenServiceImpl;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    @Test
    void storeRefreshToken_shouldSaveHashedTokenWithPrefixAndExpiry() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);
        ReflectionTestUtils.setField(service, "refreshTokenDurationMs", 60_000L);

        AppUser user = new AppUser();
        String raw = "raw-refresh-token-value";
        when(hasher.hashToken(raw)).thenReturn("hashed-value-1234567890");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        // act
        service.storeRefreshToken(user, raw);

        // assert
        verify(repo).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertSame(user, saved.getUser());
        assertEquals("hashed-value-1234567890", saved.getToken());
        assertEquals("raw-refr", saved.getTokenPrefix());
        assertNotNull(saved.getExpiryDate());
        assertTrue(saved.getExpiryDate().isAfter(Instant.now()));
    }

    @Test
    void verifyAndUpdateToken_shouldThrow_whenTokenNotFound() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        when(repo.findByTokenPrefix("raw-toke")).thenReturn(List.of());

        // act
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyAndUpdateToken("raw-token"));

        // assert
        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    void verifyAndUpdateToken_shouldThrow_whenNoHashMatches() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        RefreshToken stored = new RefreshToken();
        stored.setToken("hashed");
        stored.setExpiryDate(Instant.now().plusSeconds(600));

        when(repo.findByTokenPrefix("raw-toke")).thenReturn(List.of(stored));
        when(hasher.matches("raw-token", "hashed")).thenReturn(false);

        // act
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyAndUpdateToken("raw-token"));

        // assert
        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    void verifyAndUpdateToken_shouldDeleteAndThrow_whenExpired() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        RefreshToken stored = new RefreshToken();
        stored.setToken("hashed");
        stored.setExpiryDate(Instant.now().minusSeconds(10));

        when(repo.findByTokenPrefix("raw-toke")).thenReturn(List.of(stored));
        when(hasher.matches("raw-token", "hashed")).thenReturn(true);

        // act
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyAndUpdateToken("raw-token"));

        // assert
        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
        verify(repo).delete(stored);
        verify(repo, never()).save(any());
    }

    @Test
    void verifyAndUpdateToken_shouldThrow_whenRevoked() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        RefreshToken stored = new RefreshToken();
        stored.setToken("hashed");
        stored.setRevoked(true);
        stored.setExpiryDate(Instant.now().plusSeconds(600));

        when(repo.findByTokenPrefix("raw-toke")).thenReturn(List.of(stored));
        when(hasher.matches("raw-token", "hashed")).thenReturn(true);

        // act
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyAndUpdateToken("raw-token"));

        // assert
        assertEquals(ErrorCode.INVALID_TOKEN, ex.getErrorCode());
        verify(repo, never()).delete(any());
        verify(repo, never()).save(any());
    }

    @Test
    void verifyAndUpdateToken_shouldUpdateLastUsed_whenValid() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        RefreshToken stored = new RefreshToken();
        stored.setToken("hashed");
        stored.setExpiryDate(Instant.now().plusSeconds(600));

        when(repo.findByTokenPrefix("raw-toke")).thenReturn(List.of(stored));
        when(hasher.matches("raw-token", "hashed")).thenReturn(true);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        // act
        service.verifyAndUpdateToken("raw-token");

        // assert
        verify(repo).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertNotNull(saved.getLastUsed());
    }

    @Test
    void revokeToken_shouldMarkTokenAsRevoked() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        RefreshToken token = new RefreshToken();
        token.setRevoked(false);

        // act
        service.revokeToken(token);

        // assert
        assertTrue(token.isRevoked());
        verify(repo).save(token);
    }

    @Test
    void revokeTokenByRawToken_shouldRevokeMatchingToken() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken("hashed");
        storedToken.setRevoked(false);

        when(repo.findByTokenPrefix("raw-toke")).thenReturn(List.of(storedToken));
        when(hasher.matches("raw-token", "hashed")).thenReturn(true);

        // act
        service.revokeTokenByRawToken("raw-token");

        // assert
        assertTrue(storedToken.isRevoked());
        verify(repo).save(storedToken);
    }

    @Test
    void revokeTokenByRawToken_shouldDoNothing_whenTokenNotFound() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        when(repo.findByTokenPrefix("raw-toke")).thenReturn(List.of());

        // act
        service.revokeTokenByRawToken("raw-token");

        // assert
        verify(repo, never()).save(any());
    }

    @Test
    void findValidTokensByUser_shouldReturnNonRevokedNonExpiredToken() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        AppUser user = new AppUser();
        RefreshToken validToken = new RefreshToken();
        validToken.setRevoked(false);
        validToken.setExpiryDate(Instant.now().plusSeconds(600));

        when(repo.findByUserAndRevokedFalse(user)).thenReturn(List.of(validToken));

        // act
        List<RefreshToken> result = service.findValidTokensByUser(user);

        // assert
        assertEquals(1, result.size());
        assertEquals(validToken, result.getFirst());
    }

    @Test
    void findValidTokensByUser_shouldReturnEmpty_whenTokenRevoked() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        AppUser user = new AppUser();
        RefreshToken revokedToken = new RefreshToken();
        revokedToken.setRevoked(true); // Actually revoked
        revokedToken.setExpiryDate(Instant.now().plusSeconds(600)); // Not expired

        when(repo.findByUserAndRevokedFalse(user)).thenReturn(List.of()); // Repository filters out revoked tokens

        // act
        List<RefreshToken> result = service.findValidTokensByUser(user);

        // assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findValidTokensByUser_shouldReturnEmpty_whenTokenExpired() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenServiceImpl(repo, hasher);

        AppUser user = new AppUser();
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setRevoked(false);
        expiredToken.setExpiryDate(Instant.now().minusSeconds(600));

        when(repo.findByUserAndRevokedFalse(user)).thenReturn(List.of(expiredToken));

        // act
        List<RefreshToken> result = service.findValidTokensByUser(user);

        // assert
        assertTrue(result.isEmpty());
    }
}
