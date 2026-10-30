package com.cmanzano.pictohabla.auth.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByTeacherIdAndRevokedFalse(UUID teacherId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true, r.revokedAt = :now WHERE r.teacher.id = :teacherId AND r.revoked = false")
    void revokeAllTeacherTokens(UUID teacherId, Instant now);

    void deleteByExpiresAtBefore(Instant now);
}
