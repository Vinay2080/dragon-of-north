package org.miniProjectTwo.DragonOfNorth.modules.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.Provider;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_auth_providers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_auth_provider_user_provider", columnNames = {"user_id", "provider"})
        },
        indexes = {
                @Index(name = "idx_user_auth_provider_provider", columnList = "provider"),
                @Index(name = "idx_user_auth_provider_provider_id", columnList = "provider_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class UserAuthProvider {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_auth_provider_user"))
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private Provider provider;

    @Column(name = "provider_id")
    private String providerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
