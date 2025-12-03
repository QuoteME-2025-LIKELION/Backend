package com.ll.demo.domain.quote.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteLikeRepository quoteLikeRepository;

    // ★ Controller에서 authorId와 content를 따로 넘겨주므로, 여기서도 따로 받아야 합니다.
    @Transactional
    public QuoteResponse createQuote(Long authorId, String content) {
        // 1. 1일 1Quote 제한 체크
        validateOneQuotePerDay(authorId);

        // 2. 저장
        Quote quote = Quote.builder()
                .authorId(authorId)
                .content(content)
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

    @Transactional
    public void likeQuote(Member member, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        // 이미 좋아요를 눌렀다면 중복 처리 하지 않음 (또는 에러 처리)
        if (quoteLikeRepository.existsByQuoteAndMember(quote, member)) {
            return;
        }

        QuoteLike quoteLike = new QuoteLike(quote, member);
        quoteLikeRepository.save(quoteLike);
    }

    // 2. 좋아요 취소
    @Transactional
    public void unlikeQuote(Member member, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        quoteLikeRepository.findByQuoteAndMember(quote, member)
                .ifPresent(quoteLikeRepository::delete);
    }

    // 글 목록 조회 메서드 - mj
    public List<QuoteResponse> getQuoteList() {
        List<Quote> quotes = quoteRepository.findAll(Sort.by(Sort.Direction.DESC, "createDate"));
        return quotes.stream()
                .map(QuoteResponse::from)
                .toList();
    }
}