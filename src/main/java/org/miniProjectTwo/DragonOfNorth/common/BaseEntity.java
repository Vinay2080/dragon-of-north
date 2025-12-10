package org.miniProjectTwo.DragonOfNorth.common;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base entity class that provides common fields and functionality for all entities in the application.
 * This class includes audit fields like created date, last modified date, created by, and last modified by.
 * It uses JPA's {@code @MappedSuperclass} to allow inheritance of these common fields.
 *
 * <p>This class is designed to be extended by other entity classes to maintain consistent
 * audit information across all entities in the application.</p>
 *
 * <p>Uses Hibernate's {@code @UuidGenerator} for generating unique identifiers and
 * Spring Data JPA's auditing features to automatically populate audit fields.</p>
 *
 * @see jakarta.persistence.Entity
 * @see org.springframework.data.jpa.domain.support.AuditingEntityListener
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

    /**
     * Unique identifier for the entity.
     * Uses Hibernate's UUID generator with a time-based strategy to ensure uniqueness.
     * This field is automatically generated and cannot be updated.
     */
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "uuid", nullable = false, unique = true, updatable = false)
    private UUID id;

    /**
     * The date and time when the entity was created.
     * Automatically set by Hibernate when the entity is first persisted.
     * This field cannot be updated and is always non-null.
     */
    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    /**
     * The date and time when the entity was last modified.
     * Automatically updated by Hibernate whenever the entity is updated.
     * This field is always non-null.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * The identifier of the user who created the entity.
     * Automatically populated by Spring Data JPA auditing.
     * This field cannot be updated and is always non-null.
     */
    @CreatedBy
    @Column(updatable = false, nullable = false)
    private String createdBy;

    /**
     * The identifier of the user who last modified the entity.
     * Automatically updated by Spring Data JPA auditing.
     * This field is always non-null.
     */
    @LastModifiedBy
    @Column(nullable = false)
    private String updatedBy;

    /**
     * Flag indicating whether the entity has been soft-deleted.
     * Defaults to false. When set to true, the entity is considered deleted
     * but remains in the database for auditing purposes.
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean deleted = false;

    @Version
    private Long version;

}
