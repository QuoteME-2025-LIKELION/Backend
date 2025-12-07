package com.ll.demo.domain.quote.repository;

import com.ll.demo.domain.quote.entity.Quote;
import io.lettuce.core.dynamic.annotation.Param;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ll.demo.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    // 1. 나의 명언 조회 (최신순)
    List<Quote> findAllByAuthorIdOrderByCreateDateDesc(Long authorId);

    // 2. 특정 날짜의 전체 명언 조회 (오늘 0시 ~ 오늘 23시 59분 사이)
    List<Quote> findAllByCreateDateBetweenOrderByCreateDateDesc(LocalDateTime start, LocalDateTime end);

    // 1일 1명언 체크용
    boolean existsByAuthorIdAndCreateDateBetween(Long authorId, LocalDateTime start, LocalDateTime end);

    // 명언 개수
    long countByAuthor(Member author);
    @Query("select ql.quote from QuoteLike ql where ql.member.id = :memberId order by ql.id desc")
    List<Quote> findQuotesLikedByMember(@Param("memberId") Long memberId);

    // 그룹원 리스트 > 명언 총 개수
    long countByAuthorIn(Collection<Member> authors);
}