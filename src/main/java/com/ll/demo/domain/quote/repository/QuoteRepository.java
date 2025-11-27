package com.ll.demo.domain.quote.repository;

import com.ll.demo.domain.quote.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
    // 특정 작성자(authorId)가 특정 기간(start~end) 사이에 쓴 글이 존재하는지 확인
    boolean existsByAuthorIdAndCreateDateBetween(Long authorId, LocalDateTime start, LocalDateTime end);
}