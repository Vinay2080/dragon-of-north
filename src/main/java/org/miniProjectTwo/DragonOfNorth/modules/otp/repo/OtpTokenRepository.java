package org.miniProjectTwo.DragonOfNorth.modules.otp.repo;

import org.miniProjectTwo.DragonOfNorth.modules.otp.model.OtpToken;
import org.miniProjectTwo.DragonOfNorth.shared.enums.IdentifierType;
import org.miniProjectTwo.DragonOfNorth.shared.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for OTP token lookup, rate-limit counting, and cleanup.
 */
@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /**
     * Returns the latest OTP token for identifier/type/purpose.
     */
    Optional<OtpToken> findTopByIdentifierAndTypeAndOtpPurposeOrderByCreatedAtDesc(
            String identifier,
            IdentifierType type,
            OtpPurpose otpPurpose
    );

    /**
     * Counts OTP requests created after the given time within the same purpose.
     */
    @Query("""
            SELECT COUNT(o)
            FROM OtpToken o
            WHERE o.identifier = :identifier
              AND o.type = :type
              AND o.otpPurpose = :otpPurpose
              AND o.createdAt >= :after
            """)
    int countByIdentifierAndTypeAndOtpPurposeCreatedAtAfter(
            String identifier,
            IdentifierType type,
            OtpPurpose otpPurpose,
            Instant after
    );

    /**
     * Invalidates all unconsumed OTP tokens for a given identifier/type/purpose.
     * Called before issuing a new OTP to ensure single-use semantics.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE OtpToken o
            SET o.consumed = true
            WHERE o.identifier = :identifier
              AND o.type = :type
              AND o.otpPurpose = :otpPurpose
              AND o.consumed = false
            """)
    void invalidateActiveTokens(String identifier, IdentifierType type, OtpPurpose otpPurpose);

    /**
     * Deletes OTP tokens expired before the cutoff timestamp.
     */
    void deleteAllByExpiresAtBefore(Instant cutoff);

}