package org.miniProjectTwo.DragonOfNorth.modules.profile.repo;

import org.miniProjectTwo.DragonOfNorth.modules.profile.model.Profile;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    boolean existsByUsernameIgnoreCase(String uniqueUsername);

    @Query("""
            select count(p) > 0
            from Profile p
            where p.appUser = :appUser
            """)
    boolean existsProfileByAppUser(AppUser appUser);

    @Query("""
            select p
            from Profile p
            where p.appUser = :appUser
            """)
    Optional<Profile> findByAppUserId(UUID userId);

    boolean existsByAppUserId(UUID userId);
}
