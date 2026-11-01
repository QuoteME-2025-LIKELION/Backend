package com.ll.demo.domain.member.member.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.type.MemberProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
}