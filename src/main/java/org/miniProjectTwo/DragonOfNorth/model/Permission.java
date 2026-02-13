package org.miniProjectTwo.DragonOfNorth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Permission entity for role-based access control.
 * <p>
 * Defines granular permissions that can be assigned to roles.
 * Supports many-to-many relationship with roles for flexible authorization.
 * Critical for implementing fine-grained security controls.
 *
 * @see Role for permission assignment
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "permissions")
public class Permission extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @ManyToMany(mappedBy = "permissions")
    Set<Role> roles = new HashSet<>();
}