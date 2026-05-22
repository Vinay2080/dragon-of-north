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
     * Finds an active session row after refresh rotation by user, device, and new token hash.
     */
    Optional<Session> findByAppUserIdAndDeviceIdAndRefreshTokenHash(UUID appUserId,
                                                                    String deviceId,
                                                                    String refreshTokenHash);

    /**
     * Finds an active (non-revoked, non-deleted, unexpired) session after refresh rotation.
     */
    @Query("""
            select s
            from Session s
            where s.appUser.id = :appUserId
              and s.deviceId = :deviceId
              and s.refreshTokenHash = :refreshTokenHash
              and s.revoked = false
              and s.deleted = false
              and s.expiryDate > :now
            """)
    Optional<Session> findLiveByAppUserIdAndDeviceIdAndRefreshTokenHash(@Param("appUserId") UUID appUserId,
                                                                        @Param("deviceId") String deviceId,
                                                                        @Param("refreshTokenHash") String refreshTokenHash,
                                                                        @Param("now") Instant now);

    @Modifying
    @Query("""
            update Session s
               set s.revoked = true
             where s.appUser.id = :userId
               and s.deviceId = :deviceId
               and s.refreshTokenHash = :refreshTokenHash
               and s.revoked = false
               and s.deleted = false
            """)
    int revokeCurrentSessionIfActive(@Param("userId") UUID userId,
                                     @Param("deviceId") String deviceId,
                                     @Param("refreshTokenHash") String refreshTokenHash);

    @Modifying
    @Query("""
            update Session s
               set s.refreshTokenHash = :newRefreshTokenHash,
                   s.lastUsedAt = :lastUsedAt
             where s.appUser.id = :userId
               and s.deviceId = :deviceId
               and s.refreshTokenHash = :oldRefreshTokenHash
               and s.revoked = false
               and s.deleted = false
               and s.expiryDate > :now
            """)
    int rotateRefreshTokenIfActive(@Param("userId") UUID userId,
                                   @Param("deviceId") String deviceId,
                                   @Param("oldRefreshTokenHash") String oldRefreshTokenHash,
                                   @Param("newRefreshTokenHash") String newRefreshTokenHash,
                                   @Param("lastUsedAt") Instant lastUsedAt,
                                   @Param("now") Instant now);

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

    @Query("""
            select count(s) > 0
            from Session s
            where s.id = :sessionId
              and s.appUser.id = :userId
              and s.revoked = false
              and s.deleted = false
              and s.expiryDate > :now
            """)
    boolean existsLiveSessionForUser(@Param("sessionId") UUID sessionId,
                                     @Param("userId") UUID userId,
                                     @Param("now") Instant now);
}
