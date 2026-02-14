package org.miniProjectTwo.DragonOfNorth.repositories;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.RefreshToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void findByTokenPrefix_shouldReturnTokens_whenPrefixExists() {
        // arrange
        String prefix = "abc123";
        RefreshToken expectedToken = createTestRefreshToken();
        List<RefreshToken> expectedTokens = List.of(expectedToken);

        when(refreshTokenRepository.findByTokenPrefix(prefix)).thenReturn(expectedTokens);

        // act
        List<RefreshToken> result = refreshTokenRepository.findByTokenPrefix(prefix);

        // assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedToken, result.getFirst());
        verify(refreshTokenRepository).findByTokenPrefix(prefix);
    }

    private RefreshToken createTestRefreshToken() {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setToken("hashed-token-value");
        token.setTokenPrefix("abc123");
        token.setUser(createTestAppUser());
        token.setCreatedAt(Instant.now());
        token.setExpiryDate(Instant.now().plusSeconds(604800)); // 7 days
        token.setLastUsed(Instant.now());
        token.setRevoked(false);
        return token;
    }

    private AppUser createTestAppUser() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        return user;
    }

    @Test
    void findByTokenPrefix_shouldReturnEmptyList_whenPrefixNotExists() {
        // arrange
        String prefix = "nonexistent";

        when(refreshTokenRepository.findByTokenPrefix(prefix)).thenReturn(List.of());

        // act
        List<RefreshToken> result = refreshTokenRepository.findByTokenPrefix(prefix);

        // assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(refreshTokenRepository).findByTokenPrefix(prefix);
    }

    @Test
    void deleteByExpiryDateBefore_shouldReturnDeletedCount() {
        // arrange
        Instant now = Instant.now();
        int expectedDeletedCount = 5;

        when(refreshTokenRepository.deleteByExpiryDateBefore(now)).thenReturn(expectedDeletedCount);

        // act
        int result = refreshTokenRepository.deleteByExpiryDateBefore(now);

        // assert
        assertEquals(expectedDeletedCount, result);
        verify(refreshTokenRepository).deleteByExpiryDateBefore(now);
    }

    @Test
    void deleteByExpiryDateBefore_shouldReturnZero_whenNoTokensExpired() {
        // arrange
        Instant now = Instant.now();

        when(refreshTokenRepository.deleteByExpiryDateBefore(now)).thenReturn(0);

        // act
        int result = refreshTokenRepository.deleteByExpiryDateBefore(now);

        // assert
        assertEquals(0, result);
        verify(refreshTokenRepository).deleteByExpiryDateBefore(now);
    }

    @Test
    void user_shouldReturnUserId_whenUserProvided() {
        // arrange
        AppUser user = createTestAppUser();
        UUID expectedUserId = user.getId();

        when(refreshTokenRepository.user(user)).thenReturn(expectedUserId);

        // act
        UUID result = refreshTokenRepository.user(user);

        // assert
        assertEquals(expectedUserId, result);
        verify(refreshTokenRepository).user(user);
    }

    @Test
    void findByTokenPrefix_shouldHandleMultipleTokens() {
        // arrange
        String prefix = "abc123";
        RefreshToken token1 = createTestRefreshToken();
        RefreshToken token2 = createTestRefreshToken();
        token2.setId(UUID.randomUUID());
        List<RefreshToken> expectedTokens = List.of(token1, token2);

        when(refreshTokenRepository.findByTokenPrefix(prefix)).thenReturn(expectedTokens);

        // act
        List<RefreshToken> result = refreshTokenRepository.findByTokenPrefix(prefix);

        // assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(token1));
        assertTrue(result.contains(token2));
        verify(refreshTokenRepository).findByTokenPrefix(prefix);
    }
}
