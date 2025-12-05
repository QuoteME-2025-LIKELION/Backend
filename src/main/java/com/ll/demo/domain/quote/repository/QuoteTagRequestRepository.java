package com.ll.demo.domain.quote.repository;

import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import com.ll.demo.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QuoteTagRequestRepository extends JpaRepository<QuoteTagRequest, Long> {
    // 특정 글에 특정 멤버가 이미 태그 요청을 했는지 확인
    boolean existsByQuoteAndRequester(Quote quote, Member requester);
}