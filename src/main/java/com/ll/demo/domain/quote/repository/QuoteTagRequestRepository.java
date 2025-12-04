package com.ll.demo.domain.quote.repository;

import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteTagRequestRepository extends JpaRepository<QuoteTagRequest, Long> {
    // 중복 요청 방지
    boolean existsByQuoteAndRequester(Quote quote, Member requester);
}