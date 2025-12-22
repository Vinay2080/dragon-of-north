package org.miniProjectTwo.DragonOfNorth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.miniProjectTwo.DragonOfNorth.enums.RoleName;

import java.util.HashSet;
import java.util.Set;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor


@Table(name = "roles")
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 50)
    private RoleName roleName;

    @Column(nullable = false, name = "system_role")
    private boolean systemRole = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @ManyToMany(mappedBy = "roles")
    private Set<AppUser> appUsers = new HashSet<>();
}

// todo javadoc