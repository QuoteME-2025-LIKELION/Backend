package com.ll.demo.domain.poke.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.poke.entity.Poke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface PokeRepository extends JpaRepository<Poke, Long> {

    // 이미 오늘 찔렀는지 확인 (중복 찌르기 방지용 - 선택 사항)
    // boolean existsBySenderIdAndReceiverIdAndCreateDateBetween(...) // 이건 나중에 필요하면 추가

    // 내가 받은 콕 찌르기 개수
    long countByReceiverId(Long receiverId);

    @Modifying
    @Query("DELETE FROM Poke p WHERE p.sender = :member OR p.receiver = :member")
    void deleteAllByMember(@Param("member") Member member);
}