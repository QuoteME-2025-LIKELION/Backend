package com.ll.demo.domain.poke.repository;

import com.ll.demo.domain.poke.entity.Poke;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PokeRepository extends JpaRepository<Poke, Long> {

    // 이미 오늘 찔렀는지 확인 (중복 찌르기 방지용 - 선택 사항)
    // boolean existsBySenderIdAndReceiverIdAndCreateDateBetween(...) // 이건 나중에 필요하면 추가

    // 특정 유저가 받은 찌르기 횟수 카운트
    long countByReceiverId(Long receiverId);
}