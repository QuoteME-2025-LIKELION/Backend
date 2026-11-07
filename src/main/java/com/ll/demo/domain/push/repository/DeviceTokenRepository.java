package com.ll.demo.domain.push.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.push.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByToken(String token);

    @Query("SELECT d.token FROM DeviceToken d WHERE d.member.id = :memberId")
    List<String> findTokensByMemberId(@Param("memberId") Long memberId);

    long countByMember(Member member);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token = :token AND d.member = :member")
    int deleteByTokenAndMember(@Param("token") String token, @Param("member") Member member);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.token IN :tokens")
    int deleteAllByTokenIn(@Param("tokens") Collection<String> tokens);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.member = :member")
    void deleteAllByMember(@Param("member") Member member);
}
