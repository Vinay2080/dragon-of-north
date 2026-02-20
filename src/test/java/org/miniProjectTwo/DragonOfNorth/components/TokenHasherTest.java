package org.miniProjectTwo.DragonOfNorth.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenHasherTest {

    private final TokenHasher tokenHasher = new TokenHasher();

    @Test
    void hashToken_shouldBeDeterministic() {
        String hash1 = tokenHasher.hashToken("same-token");
        String hash2 = tokenHasher.hashToken("same-token");

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
    }

    @Test
    void hashToken_shouldProduceDifferentHashesForDifferentInput() {
        String hash1 = tokenHasher.hashToken("token-a");
        String hash2 = tokenHasher.hashToken("token-b");

        assertNotEquals(hash1, hash2);
    }
}
