package com.crego.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    Optional<EmailVerificationToken> findFirstByClientIdOrderByCreatedAtDesc(Long clientId);

    // Marks the token used only if nobody else did first; returns 0 when another request won
    @Modifying
    @Query("update EmailVerificationToken t set t.usedAt = :now where t.id = :id and t.usedAt is null")
    int markUsed(@Param("id") Long id, @Param("now") Instant now);

    // When a new link is sent, older unused links stop working
    @Modifying
    @Query("update EmailVerificationToken t set t.usedAt = :now where t.client.id = :clientId and t.usedAt is null")
    void invalidateUnused(@Param("clientId") Long clientId, @Param("now") Instant now);
}
