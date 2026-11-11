package com.ll.demo.global.security.oauth2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OAuth2ExchangeCodeRepository extends JpaRepository<OAuth2ExchangeCode, Long> {

    Optional<OAuth2ExchangeCode> findByCodeHashAndPurpose(String codeHash, OAuth2ExchangeCode.Purpose purpose);

    // 1회용 사용 처리: 영향 행이 1이어야 성공 (동시 요청 중 하나만 성공)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OAuth2ExchangeCode c SET c.usedAt = :now"
            + " WHERE c.codeHash = :codeHash AND c.purpose = :purpose AND c.usedAt IS NULL AND c.expiresAt > :now")
    int markUsed(@Param("codeHash") String codeHash,
                 @Param("purpose") OAuth2ExchangeCode.Purpose purpose,
                 @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OAuth2ExchangeCode c WHERE c.expiresAt < :now OR c.usedAt IS NOT NULL")
    int deleteExpiredOrUsed(@Param("now") LocalDateTime now);
}
