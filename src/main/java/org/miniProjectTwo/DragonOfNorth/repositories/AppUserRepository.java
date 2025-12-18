package org.miniProjectTwo.DragonOfNorth.repositories;

import lombok.NonNull;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<@NonNull AppUser, @NonNull UUID> {

    @NullMarked
    Optional<AppUser> findById(UUID uuid);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByPhoneNumber(String phoneNumber);

    Optional<AppUserStatus> findAppUserStatusByEmail(String email);

}
//todo javadoc