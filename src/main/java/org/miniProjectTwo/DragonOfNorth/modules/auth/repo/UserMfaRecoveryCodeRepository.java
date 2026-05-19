package org.miniProjectTwo.DragonOfNorth.modules.auth.repo;

import jakarta.persistence.LockModeType;
import org.miniProjectTwo.DragonOfNorth.modules.auth.model.UserMfaRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserMfaRecoveryCodeRepository extends JpaRepository<UserMfaRecoveryCode, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<UserMfaRecoveryCode> findByMfaSettingsIdAndUsedFalseAndDeletedFalse(UUID mfaSettingsId);

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
