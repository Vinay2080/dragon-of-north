package org.miniProjectTwo.DragonOfNorth.modules.auth.repo;

import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserMfaRecoveryCodeRepository extends JpaRepository<UserMfaRecoveryCode, UUID> {

    List<UserMfaRecoveryCode> findByMfaSettingsIdAndUsedFalseAndDeletedFalse(UUID mfaSettingsId);

    @Modifying
    @Query("""
            update UserMfaRecoveryCode code
               set code.used = true,
                   code.usedAt = :usedAt
             where code.id = :recoveryCodeId
               and code.used = false
               and code.deleted = false
            """)
    int consumeIfUnused(@Param("recoveryCodeId") UUID recoveryCodeId, @Param("usedAt") Instant usedAt);

    /**
     * Marks all active (unused and not deleted) recovery codes for the given MFA settings as used, effectively invalidating them. This is typically called when new recovery codes are generated to ensure that any previously issued codes cannot be used.
     *
     * @param mfaSettingsId The ID of the MFA settings for which to invalidate active recovery codes. Must not be null.
     * @param usedAt        The timestamp to set as the usedAt value for the invalidated codes. Typically, the current time. Must not be null.
     */
    @Modifying
    @Query("""
            update UserMfaRecoveryCode code
               set code.used = true,
                   code.usedAt = :usedAt
             where code.mfaSettings.id = :mfaSettingsId
               and code.used = false
            """)
    void invalidateActiveCodes(@Param("mfaSettingsId") UUID mfaSettingsId, @Param("usedAt") Instant usedAt);
}
