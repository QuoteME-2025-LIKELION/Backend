package com.ll.demo.domain.quote.service;

import com.ll.demo.domain.quote.dto.QuoteCreateRequest;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;

    @Transactional
    public QuoteResponse createQuote(QuoteCreateRequest request) {
        // 1. 1일 1Quote 제한 체크
        validateOneQuotePerDay(request.getAuthorId());

        // 2. 저장
        Quote quote = Quote.builder()
                .authorId(request.getAuthorId())
                .content(request.getContent())
                .build();

        Quote savedQuote = quoteRepository.save(quote);

        return QuoteResponse.from(savedQuote);
    }

    private void validateOneQuotePerDay(Long authorId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        boolean hasQuoteToday = quoteRepository.existsByAuthorIdAndCreateDateBetween(authorId, startOfDay, endOfDay);

        if (hasQuoteToday) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "오늘 이미 명언을 작성하셨습니다.");
        }
    }
}