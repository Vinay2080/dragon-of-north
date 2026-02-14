package org.miniProjectTwo.DragonOfNorth.repositories;


import org.miniProjectTwo.DragonOfNorth.model.AppUser;
import org.miniProjectTwo.DragonOfNorth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    List<RefreshToken> findByTokenPrefix(String prefix);

    int deleteByExpiryDateBefore(Instant now);

    List<RefreshToken> findByUserAndRevokedFalse(AppUser appUser);

    int deleteByRevokedTrueAndCreatedAtBefore(Instant cutoff);

    UUID user(AppUser user);

    void deleteByUser(AppUser appUser);
}
