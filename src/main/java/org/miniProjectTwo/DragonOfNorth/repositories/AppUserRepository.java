package org.miniProjectTwo.DragonOfNorth.repositories;

import lombok.NonNull;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<@NonNull AppUser, @NonNull UUID> {

    @NullMarked
    Optional<AppUser> findById(UUID uuid);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByPhone(String phone);

    @Query("select a.appUserStatus from AppUser a where a.email = :email")
    Optional<AppUserStatus> findAppUserStatusByEmail(String email);

    @Query("select a.appUserStatus from AppUser a where a.phone = :phone")
    Optional<AppUserStatus> findAppUserStatusByPhone(String phone);

    void deleteByAppUserStatusAndCreatedAtBefore(AppUserStatus appUserStatus, Instant createdAtBefore);


    @Query("select u.roles from AppUser u where u.id = :userId")
    Set<Role> findRolesById(@Param("userId") UUID uuid);

}

