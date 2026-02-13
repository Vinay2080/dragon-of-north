package org.miniProjectTwo.DragonOfNorth.services;

import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.components.TokenHasher;
import org.miniProjectTwo.DragonOfNorth.enums.ErrorCode;
import org.miniProjectTwo.DragonOfNorth.exception.BusinessException;
import org.miniProjectTwo.DragonOfNorth.impl.RefreshTokenService;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.RefreshToken;
import org.miniProjectTwo.DragonOfNorth.repositories.RefreshTokenRepository;
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
        RefreshTokenService service = new RefreshTokenService(repo, hasher);
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
        assertEquals("hashed-v", saved.getTokenPrefix());
        assertNotNull(saved.getExpiryDate());
        assertTrue(saved.getExpiryDate().isAfter(Instant.now()));
    }

    @Test
    void verifyAndUpdateToken_shouldThrow_whenTokenNotFound() {
        // arrange
        RefreshTokenRepository repo = mock(RefreshTokenRepository.class);
        TokenHasher hasher = mock(TokenHasher.class);
        RefreshTokenService service = new RefreshTokenService(repo, hasher);

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
        RefreshTokenService service = new RefreshTokenService(repo, hasher);

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
        RefreshTokenService service = new RefreshTokenService(repo, hasher);

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
        RefreshTokenService service = new RefreshTokenService(repo, hasher);

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
        RefreshTokenService service = new RefreshTokenService(repo, hasher);

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
}
