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

/**
 * Repository for device-session lookup, revocation, and cleanup operations.
 */
public interface SessionRepository extends JpaRepository<Session, UUID> {

    /**
     * Finds a session by token hash, device id, and owning user.
     */
    Optional<Session> findByRefreshTokenHashAndDeviceIdAndAppUser(String refreshTokenHash, String deviceId, AppUser appUser);

    /**
     * Finds a session for the given user/device pair.
     */
    Optional<Session> findByAppUserAndDeviceId(AppUser appUser, String deviceId);

    /**
     * Lists all sessions for a user ordered by recent usage.
     */
    List<Session> findAllByAppUserIdOrderByLastUsedAtDesc(UUID appUserId);

    /**
     * Finds one session by session id constrained to user ownership.
     */
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

    /**
     * Deletes expired sessions older than the current time.
     */
    long deleteByExpiryDateBefore(Instant now);

    /**
     * Deletes revoked sessions retained beyond the configured cutoff.
     */
    long deleteByRevokedTrueAndUpdatedAtBefore(Instant cutoff);

    /**
     * Revokes all non-revoked sessions for the user.
     */
    @Modifying
    @Query("""
            update Session s
            set s.revoked = true
            where s.appUser.id = :userId
                and s.revoked = false
            """)
    int revokeAllSessionsByUserId(UUID userId);
}