package com.ll.demo.domain.quote.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository; // [추가] 필수!
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import com.ll.demo.domain.quote.repository.QuoteTagRequestRepository;
import com.ll.demo.global.exceptions.GlobalException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteLikeRepository quoteLikeRepository;
    private final MemberRepository memberRepository; // ★ [수정] 이게 없어서 에러가 났었습니다!
    private final QuoteTagRequestRepository quoteTagRequestRepository;

    // 명언 작성 (저장)
    @Transactional
    public QuoteResponse createQuote(Long authorId, String content, String originalContent) {
        // 1. 1일 1명언 제한 체크
        validateOneQuotePerDay(authorId);

        // 2. 회원(Member) 조회
        // (이제 위에서 memberRepository를 선언했으므로 에러가 안 납니다)
        Member author = memberRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 3. 빌더 패턴으로 명언 객체 생성
        Quote quote = Quote.builder()
                .author(author)
                .content(content)
                .originalContent(originalContent) // 원본 일기 내용도 저장
                .build();

        quoteRepository.save(quote);

        return new QuoteResponse(quote);
    }

    private void validateOneQuotePerDay(Long authorId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        boolean hasQuoteToday = quoteRepository.existsByAuthorIdAndCreateDateBetween(authorId, startOfDay, endOfDay);

        if (hasQuoteToday) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "오늘 이미 명언을 작성하셨습니다.");
        }
    }

    // 좋아요 등록
    @Transactional
    public void likeQuote(Member member, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        if (quoteLikeRepository.existsByQuoteAndMember(quote, member)) {
            return;
        }

        QuoteLike quoteLike = new QuoteLike(quote, member);
        quoteLikeRepository.save(quoteLike);
    }

    // 좋아요 취소
    @Transactional
    public void unlikeQuote(Member member, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        quoteLikeRepository.findByQuoteAndMember(quote, member)
                .ifPresent(quoteLikeRepository::delete);
    }

    // 전체 명언 목록 조회 (단순 리스트)
    public List<QuoteResponse> getQuoteList() {
        List<Quote> quotes = quoteRepository.findAll(Sort.by(Sort.Direction.DESC, "createDate"));
        return quotes.stream()
                .map(QuoteResponse::from)
                .toList();
    }

    // 명언에 태그 요청하는 메서드 - mj
    @Transactional
    public void tagRequestQuote(Member requester, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "명언을 찾을 수 없습니다."));

        if (quoteTagRequestRepository.existsByQuoteAndRequester(quote, requester)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 해당 글에 태그 요청을 하셨습니다.");
        }

        // 자기 글에 요청하는지 체크
        // 지금은 임시로 ID로 비교한다 가정
        if (requester.getId().equals(quote.getAuthor().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인 글에는 태그 요청을 할 수 없습니다.");
        }

        QuoteTagRequest tagRequest = QuoteTagRequest.builder()
                .quote(quote)
                .requester(requester)
                .build();

        quoteTagRequestRepository.save(tagRequest);
    }

    // 1. 나의 명언 목록 가져오기
    public List<QuoteResponse> findMyQuotes(Long authorId) {
        List<Quote> quotes = quoteRepository.findAllByAuthorIdOrderByCreateDateDesc(authorId);

        // ★ 여기서(Service 내부) 변환해야 DB 연결이 유지된 상태로 닉네임을 가져올 수 있음
        return quotes.stream()
                .map(QuoteResponse::new)
                .toList();
    }

    // 2. 특정 날짜의 전체 명언 가져오기
    public List<QuoteResponse> findQuotesByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Quote> quotes = quoteRepository.findAllByCreateDateBetweenOrderByCreateDateDesc(startOfDay, endOfDay);

        return quotes.stream()
                .map(QuoteResponse::new)
                .toList();
    }

    // 태그 요청
    @Transactional
    public void requestTagToQuote(Long quoteId, Member requester) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new GlobalException("404", "해당 명언을 찾을 수 없습니다."));
        if (quote.getAuthor().getId().equals(requester.getId())) {
            throw new GlobalException("400", "자신이 작성한 글에는 태그 요청을 할 수 없습니다.");
        }
        if (quoteTagRequestRepository.existsByQuoteAndRequester(quote, requester)) {
            throw new GlobalException("409", "이미 해당 명언에 태그 요청을 하였습니다.");
        }
        QuoteTagRequest tagRequest = QuoteTagRequest.builder()
                .quote(quote)
                .requester(requester)
                .build();
        quoteTagRequestRepository.save(tagRequest);
    }
}