package org.miniProjectTwo.DragonOfNorth.repositories;

import org.miniProjectTwo.DragonOfNorth.enums.OtpType;
import org.miniProjectTwo.DragonOfNorth.model.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository interface for managing OTP (One-Time Password) tokens.
 * Provides methods to interact with the underlying database for OTP operations.
 *
 * @see OtpToken
 * @see OtpType
 */
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    
    /**
     * Retrieves the most recent OTP token for the given identifier and type.
     *
     * @param identifier The unique identifier (e.g., email or phone number)
     * @param type The type of OTP (e.g., EMAIL, SMS)
     * @return An {@link Optional} containing the most recent OTP token if found, or empty if none exists
     */
    Optional<OtpToken> findTopByIdentifierAndTypeOrderByCreatedAtDesc(
            String identifier,
            OtpType type
    );

    /**
     * Counts the number of OTP requests for a specific identifier and type
     * that were created after the specified timestamp.
     *
     * @param identifier The unique identifier to search for
     * @param type The type of OTP to filter by
     * @param after The cutoff timestamp (inclusive)
     * @return The count of matching OTP tokens
     */
    @Query("""
            SELECT COUNT(o)
            FROM OtpToken o
            WHERE o.identifier = :identifier
              AND o.type = :type
              AND o.createdAt >= :after
            """)
    int countByIdentifierAndTypeAndCreatedAtAfter(
            String identifier,
            OtpType type,
            Instant after
    );

    /**
     * Deletes all OTP tokens that have expired before the specified cutoff time.
     * This is typically used for cleaning up expired tokens.
     *
     * @param cutoff The timestamp before which tokens are considered expired
     */
    void deleteAllByExpiresAtBefore(Instant cutoff);
}