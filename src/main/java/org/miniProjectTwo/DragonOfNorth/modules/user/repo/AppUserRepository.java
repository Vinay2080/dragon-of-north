package org.miniProjectTwo.DragonOfNorth.modules.user.repo;

import lombok.NonNull;
import org.jspecify.annotations.NullMarked;
import org.miniProjectTwo.DragonOfNorth.modules.user.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.shared.enums.AppUserStatus;
import org.miniProjectTwo.DragonOfNorth.shared.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for user lookup, state queries, and maintenance operations.
 */
@Repository
public interface AppUserRepository extends JpaRepository<@NonNull AppUser, @NonNull UUID> {

    /**
     * Finds a user by primary key.
     *
     * @param uuid user id
     * @return user when present
     */
    @NullMarked
    Optional<AppUser> findById(UUID uuid);

    /**
     * Finds a user by email identifier.
     *
     * @param email email identifier
     * @return user when present
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * Finds a user by phone identifier.
     *
     * @param phone phone identifier
     * @return user when present
     */
    Optional<AppUser> findByPhone(String phone);

    /**
     * Locks the matching email row for update within the active transaction.
     *
     * @param email email identifier
     * @return locked user when present
     */
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.email = :email")
    Optional<AppUser> findByEmailForUpdate(@Param("email") String email);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.phone = :phone")
    Optional<AppUser> findByPhoneForUpdate(@Param("phone") String phone);

    /**
     * Reads account status by email without loading the full entity.
     *
     * @param email email identifier
     * @return account status when the user exists
     */
    @Query("select a.appUserStatus from AppUser a where a.email = :email")
    Optional<AppUserStatus> findAppUserStatusByEmail(String email);

    /**
     * Reads account status by phone without loading the full entity.
     *
     * @param phone phone identifier
     * @return account status when the user exists
     */
    @Query("select a.appUserStatus from AppUser a where a.phone = :phone")
    Optional<AppUserStatus> findAppUserStatusByPhone(String phone);

    /**
     * Deletes unverified users older than the given creation timestamp.
     *
     * @param createdAtBefore cutoff timestamp
     * @return number of deleted users
     */
    long deleteByIsEmailVerifiedFalseAndCreatedAtBefore(Instant createdAtBefore);


    /**
     * Loads roles assigned to a user.
     *
     * @param uuid user id
     * @return assigned roles (possibly empty)
     */
    @Query("select u.roles from AppUser u where u.id = :userId")
    Set<Role> findRolesById(@Param("userId") UUID uuid);

    /**
     * Checks an email-verification flag by user id.
     *
     * @param id user id
     * @return email verification state
     */
    @Query("select u.isEmailVerified from AppUser u where u.id = :id")
    boolean isEmailVerified(@Param("id") UUID id);

    @Query("select coalesce(u.email, u.phone) from AppUser u where u.id = :id")
    Optional<String> findPreferredIdentifierById(@Param("id") UUID id);
}
