package com.ll.demo.domain.member.member.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.type.MemberProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email); // 이메일? 아이디?
    Optional<Member> findByProviderAndProviderId(MemberProvider provider, String providerId);
    Optional<Member> findByRefreshToken(String refreshToken);
    Optional<Member> findByNickname(String nickname);

    // 닉네임 또는 이메일 - 전체 검색으로만 추출되므로 삭제
    // List<Member> findByNicknameContainingOrEmailContaining(String nicknameKeyword, String emailKeyword);
    // 부분 검색
    @Query("""
        SELECT m FROM Member m 
        WHERE LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!'
        OR LOWER(m.email) LIKE LOWER(CONCAT(:keyword, '@%')) ESCAPE '!'
        OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '@%')) ESCAPE '!'
    """)
    // keyword는 '!'로 LIKE 이스케이프(!, %, _)된 값이어야 함
    List<Member> searchMembersByNicknameOrEmailUsername(@Param("keyword") String keyword);

    @Query("SELECT m FROM Member m WHERE m.provider <> com.ll.demo.domain.member.member.type.MemberProvider.LOCAL AND m.providerId IS NOT NULL")
    List<Member> findAllWithSocialProvider();

    // MySQL은 SET을 왼쪽부터 평가하므로 aiUsageCount를 aiUsageDate보다 먼저 둔다
    @Modifying
    @Query("""
        UPDATE Member m
        SET m.aiUsageCount = CASE WHEN m.aiUsageDate = :today THEN m.aiUsageCount + 1 ELSE 1 END,
            m.aiUsageDate = :today
        WHERE m.id = :id
        AND (m.aiUsageDate IS NULL OR m.aiUsageDate <> :today OR m.aiUsageCount < :limit)
    """)
    int incrementAiUsage(@Param("id") Long id, @Param("today") LocalDate today, @Param("limit") int limit);
}