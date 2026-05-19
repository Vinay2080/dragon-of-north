package org.miniProjectTwo.DragonOfNorth.modules.auth.repo;

import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserMfaSettingsRepository extends JpaRepository<UserMfaSettings, UUID> {

    Optional<UserMfaSettings> findByUserId(UUID userId);
}
