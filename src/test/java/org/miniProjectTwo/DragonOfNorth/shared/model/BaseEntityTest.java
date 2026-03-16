package org.miniProjectTwo.DragonOfNorth.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.miniProjectTwo.DragonOfNorth.shared.model.BaseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BaseEntityTest {

    private BaseEntity baseEntity;

    @BeforeEach
    void setUp() {
        baseEntity = new BaseEntity();
    }

    @Test
    void constructor_shouldCreateEmptyBaseEntity() {
        // assert
        assertNotNull(baseEntity);
        assertNull(baseEntity.getId());
        assertNull(baseEntity.getCreatedAt());
        assertNull(baseEntity.getUpdatedAt());
        assertNull(baseEntity.getCreatedBy());
        assertNull(baseEntity.getUpdatedBy());
        assertFalse(baseEntity.getDeleted());
        assertNull(baseEntity.getVersion());
    }

    @Test
    void constructorWithAllArgs_shouldCreateBaseEntity() {
        // arrange
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now().plusSeconds(60);
        String createdBy = "user1";
        String updatedBy = "user2";
        Boolean deleted = false;
        Long version = 1L;

        // act
        BaseEntity entity = new BaseEntity();
        entity.setId(id);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);
        entity.setCreatedBy(createdBy);
        entity.setUpdatedBy(updatedBy);
        entity.setDeleted(deleted);
        entity.setVersion(version);

        // assert
        assertEquals(id, entity.getId());
        assertEquals(createdAt, entity.getCreatedAt());
        assertEquals(updatedAt, entity.getUpdatedAt());
        assertEquals(createdBy, entity.getCreatedBy());
        assertEquals(updatedBy, entity.getUpdatedBy());
        assertEquals(deleted, entity.getDeleted());
        assertEquals(version, entity.getVersion());
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // arrange
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        String user = "testUser";

        // act
        baseEntity.setId(id);
        baseEntity.setCreatedAt(now);
        baseEntity.setUpdatedAt(now.plusSeconds(30));
        baseEntity.setCreatedBy(user);
        baseEntity.setUpdatedBy(user + "_updated");
        baseEntity.setDeleted(true);
        baseEntity.setVersion(5L);

        // assert
        assertEquals(id, baseEntity.getId());
        assertEquals(now, baseEntity.getCreatedAt());
        assertEquals(now.plusSeconds(30), baseEntity.getUpdatedAt());
        assertEquals(user, baseEntity.getCreatedBy());
        assertEquals(user + "_updated", baseEntity.getUpdatedBy());
        assertTrue(baseEntity.getDeleted());
        assertEquals(5L, baseEntity.getVersion());
    }

    @Test
    void deleted_shouldDefaultToFalse() {
        // assert
        assertFalse(baseEntity.getDeleted());
    }

    @Test
    void deleted_shouldAcceptTrue() {
        // act
        baseEntity.setDeleted(true);

        // assert
        assertTrue(baseEntity.getDeleted());
    }

    @Test
    void version_shouldAcceptNull() {
        // act
        baseEntity.setVersion(null);

        // assert
        assertNull(baseEntity.getVersion());
    }

    @Test
    void version_shouldAcceptValue() {
        // act
        baseEntity.setVersion(10L);

        // assert
        assertEquals(10L, baseEntity.getVersion());
    }

    @Test
    void id_shouldAcceptNull() {
        // act
        baseEntity.setId(null);

        // assert
        assertNull(baseEntity.getId());
    }

    @Test
    void id_shouldAcceptValue() {
        // arrange
        UUID id = UUID.randomUUID();

        // act
        baseEntity.setId(id);

        // assert
        assertEquals(id, baseEntity.getId());
    }

    @Test
    void timestamps_shouldAcceptNull() {
        // act
        baseEntity.setCreatedAt(null);
        baseEntity.setUpdatedAt(null);

        // assert
        assertNull(baseEntity.getCreatedAt());
        assertNull(baseEntity.getUpdatedAt());
    }

    @Test
    void auditFields_shouldAcceptNull() {
        // act
        baseEntity.setCreatedBy(null);
        baseEntity.setUpdatedBy(null);

        // assert
        assertNull(baseEntity.getCreatedBy());
        assertNull(baseEntity.getUpdatedBy());
    }

    @Test
    void auditFields_shouldAcceptValues() {
        // arrange
        String creator = "creator";
        String updater = "updater";

        // act
        baseEntity.setCreatedBy(creator);
        baseEntity.setUpdatedBy(updater);

        // assert
        assertEquals(creator, baseEntity.getCreatedBy());
        assertEquals(updater, baseEntity.getUpdatedBy());
    }
}
