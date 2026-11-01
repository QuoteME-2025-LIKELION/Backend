package com.ll.demo.domain.member.member.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.entity.MemberSocialAccount;
import com.ll.demo.domain.member.member.type.MemberProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberSocialAccountRepository extends JpaRepository<MemberSocialAccount, Long> {

    // 트랜잭션 밖에서 회원을 쓰므로 함께 조회
    @EntityGraph(attributePaths = "member")
    Optional<MemberSocialAccount> findByProviderAndProviderId(MemberProvider provider, String providerId);

    boolean existsByMemberAndProvider(Member member, MemberProvider provider);

    List<MemberSocialAccount> findAllByMemberOrderById(Member member);

    @Modifying
    @Query("DELETE FROM MemberSocialAccount a WHERE a.member = :member")
    void deleteAllByMember(@Param("member") Member member);
}
