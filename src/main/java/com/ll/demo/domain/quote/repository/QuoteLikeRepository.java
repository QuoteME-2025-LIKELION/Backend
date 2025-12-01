package com.ll.demo.domain.quote.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuoteLikeRepository extends JpaRepository<QuoteLike, Long> {
    // 특정 명언에 특정 멤버가 좋아요를 눌렀는지 확인
    boolean existsByQuoteAndMember(Quote quote, Member member);

    // 좋아요 취소를 위해 데이터 조회
    Optional<QuoteLike> findByQuoteAndMember(Quote quote, Member member);
}