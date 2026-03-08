package org.miniProjectTwo.DragonOfNorth.modules.session.repo;

import org.miniProjectTwo.DragonOfNorth.modules.session.model.Session;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByRefreshTokenHashAndDeviceIdAndAppUser(String refreshTokenHash, String deviceId, AppUser appUser);

    Optional<Session> findByAppUserAndDeviceId(AppUser appUser, String deviceId);

    List<Session> findAllByAppUserIdOrderByLastUsedAtDesc(UUID appUserId);

    Optional<Session> findByIdAndAppUserId(UUID sessionId, UUID userId);

    @Modifying
    @Query("""
            update Session s
            set s.revoked = true
            where s.appUser.id = :userId
                and s.deviceId <> :currentDeviceId
                    and s.revoked = false
            """)
    int revokeAllOtherSessions(@Param("userId") UUID userId,
                               @Param("currentDeviceId") String currentDeviceId);

    long deleteByExpiryDateBefore(Instant now);

    long deleteByRevokedTrueAndUpdatedAtBefore(Instant cutoff);

    @Modifying
    @Query("""
            update Session s
            set s.revoked = true
            where s.appUser.id = :userId
                and s.revoked = false
            """)
    int revokeAllSessionsByUserId(UUID userId);
}