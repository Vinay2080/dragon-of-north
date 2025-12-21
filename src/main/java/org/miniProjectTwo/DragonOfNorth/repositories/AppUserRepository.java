package org.miniProjectTwo.DragonOfNorth.repositories;

import lombok.NonNull;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<@NonNull AppUser, @NonNull UUID> {

    @NullMarked
    Optional<AppUser> findById(UUID uuid);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByPhoneNumber(String phoneNumber);

    Optional<AppUserStatus> findAppUserStatusByEmail(String email);

    @Modifying
    @Query("""
            update AppUser  u
            set u.appUserStatus = :status
            where u.id = :id
            """)
    int updateUserStatusById(@Param("id") UUID uuid, @Param("status") AppUserStatus appUserStatus);


}
//todo javadoc