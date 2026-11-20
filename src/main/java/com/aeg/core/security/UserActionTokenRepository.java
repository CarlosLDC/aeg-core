package com.aeg.core.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface UserActionTokenRepository extends JpaRepository<UserActionToken, Long> {

    @Query("""
            SELECT t FROM UserActionToken t
            JOIN FETCH t.user
            WHERE t.tokenHash = :tokenHash AND t.purpose = :purpose
            """)
    Optional<UserActionToken> findByTokenHashAndPurposeWithUser(
            @Param("tokenHash") String tokenHash,
            @Param("purpose") UserActionTokenPurpose purpose
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserActionToken t
            SET t.usedAt = :now
            WHERE t.user.id = :userId
              AND t.purpose = :purpose
              AND t.usedAt IS NULL
            """)
    int invalidateActiveTokens(
            @Param("userId") Long userId,
            @Param("purpose") UserActionTokenPurpose purpose,
            @Param("now") OffsetDateTime now
    );

    void deleteByUser_Id(Long userId);

    @Modifying
    @Query("DELETE FROM UserActionToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") OffsetDateTime cutoff);
}
