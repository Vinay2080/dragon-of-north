package org.miniProjectTwo.DragonOfNorth.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenHasherTest {

    @Test
    void hashToken_and_matches_shouldWork() {
        // arrange
        TokenHasher hasher = new TokenHasher();
        String raw = "refresh-token-raw-value";

        // act
        String hashed = hasher.hashToken(raw);

        // assert
        assertNotNull(hashed);
        assertNotEquals(raw, hashed);
        assertTrue(hasher.matches(raw, hashed));
        assertFalse(hasher.matches("wrong", hashed));
    }
}
