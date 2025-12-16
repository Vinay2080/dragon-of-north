package org.miniProjectTwo.DragonOfNorth.repositories;

import lombok.NonNull;
import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppUserRepository extends JpaRepository<@NonNull AppUser, @NonNull String> {

    AppUser findByEmailIgnoreCaseOrPhoneNumberIgnoreCase(String username, String username1);


    AppUser findById(UUID uuid);
}
//todo javadoc