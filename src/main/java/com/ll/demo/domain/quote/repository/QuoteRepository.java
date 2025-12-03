package com.ll.demo.domain.quote.repository;

import com.ll.demo.domain.quote.entity.Quote;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    // 1. 나의 명언 조회 (최신순)
    List<Quote> findAllByAuthorIdOrderByCreateDateDesc(Long authorId);

    // 2. 특정 날짜의 전체 명언 조회 (오늘 0시 ~ 오늘 23시 59분 사이)
    List<Quote> findAllByCreateDateBetweenOrderByCreateDateDesc(LocalDateTime start, LocalDateTime end);

    // 1일 1명언 체크용
    boolean existsByAuthorIdAndCreateDateBetween(Long authorId, LocalDateTime start, LocalDateTime end);
}