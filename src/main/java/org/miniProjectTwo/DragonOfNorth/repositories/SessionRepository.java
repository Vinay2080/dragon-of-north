package org.miniProjectTwo.DragonOfNorth.repositories;

import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByRefreshTokenHashAndDeviceIdAndAppUser(String refreshTokenHash, String deviceId, AppUser appUser);

    Optional<Session> findByAppUserAndDeviceId(AppUser appUser, String deviceId);
}