package com.ll.demo.domain.quote.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.dto.QuoteTagRequestResponse;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
import com.ll.demo.domain.quote.entity.QuoteTag;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import com.ll.demo.domain.quote.entity.TagRequestStatus;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRequestRepository;
import com.ll.demo.global.exceptions.GlobalException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteLikeRepository quoteLikeRepository;
    private final MemberRepository memberRepository; // ★ [수정] 이게 없어서 에러가 났었습니다!
    private final QuoteTagRequestRepository quoteTagRequestRepository;
    private final QuoteTagRepository quoteTagRepository;
    private final NotificationService notificationService;


    // 명언 작성 (저장)
    @Transactional
    public QuoteResponse createQuote(Long authorId, String content, String originalContent, List<Long> taggedMemberIds) {
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

        if (taggedMemberIds != null && !taggedMemberIds.isEmpty()) {
            for (Long memberId : taggedMemberIds) {
                Member taggedMember = memberRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("태그된 회원을 찾을 수 없습니다."));

                // 태그 저장
                QuoteTag quoteTag = new QuoteTag(quote, taggedMember);
                quoteTagRepository.save(quoteTag);

                // 알림 발송 (TAG 타입)
                notificationService.create(
                        taggedMember,
                        author,
                        "TAG",
                        author.getName() + "님이 글에 태그했습니다.",
                        quote.getId()
                );
            }
        }

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


    // [추가] 좋아요한 글 목록 조회
    public List<QuoteResponse> findLikedQuotes(Long memberId) {
        // 1. DB에서 내가 좋아요한 Quote 목록 조회
        List<Quote> likedQuotes = quoteRepository.findQuotesLikedByMember(memberId);

        // 2. DTO로 변환
        return likedQuotes.stream()
                .map(QuoteResponse::new)
                .toList();
    }

    // 태그 수정 기능
    @Transactional
    public void updateTags(Long authorId, Long quoteId, List<Long> taggedMemberIds) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        // 1. 작성자 본인 확인 (본인 글만 태그 수정 가능)
        if (!quote.getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        // 2. 기존 태그 싹 지우기 (초기화)
        quoteTagRepository.deleteAllByQuote(quote);

        // 3. 새로운 태그 저장 및 알림 발송
        if (taggedMemberIds != null && !taggedMemberIds.isEmpty()) {
            for (Long memberId : taggedMemberIds) {
                Member taggedMember = memberRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("존재하지 않는 회원입니다."));

                // 태그 저장
                QuoteTag quoteTag = new QuoteTag(quote, taggedMember);
                quoteTagRepository.save(quoteTag);

                // ★ 알림 발송 (TAG 타입)
                // 내용: "OO님이 회원님을 글에 태그했습니다."
                // 클릭 시 이동(targetId): 해당 명언 ID (quote.getId())
                notificationService.create(
                        taggedMember,       // 받는 사람 (태그된 친구)
                        quote.getAuthor(),  // 보낸 사람 (글쓴이)
                        "TAG",
                        quote.getAuthor().getName() + "님이 글에 태그했습니다.",
                        quote.getId()
                );
            }
        }
    }
    // [통합 & 수정] 태그 요청 기능 (알림 기능 추가됨)
    @Transactional
    public void requestTag(Long requesterId, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        // 1. 검증: 본인 글에는 요청 불가
        if (quote.getAuthor().getId().equals(requesterId)) {
            throw new RuntimeException("본인 글에는 태그 요청을 할 수 없습니다.");
        }

        // 2. 검증: 이미 요청했는지 확인
        if (quoteTagRequestRepository.existsByQuoteAndRequester(quote, requester)) {
            throw new RuntimeException("이미 태그 요청을 보냈습니다.");
        }

        // 3. 요청 저장
        QuoteTagRequest request = QuoteTagRequest.builder()
                .quote(quote)
                .requester(requester)
                .status(TagRequestStatus.PENDING) // Enum에 PENDING이 있어야 합니다.
                .build();

        quoteTagRequestRepository.save(request);

        // 4. ★ 알림 발송 (작성자에게)
        // Type: "TAG_REQUEST"
        // TargetId: 해당 명언 ID
        notificationService.create(
                quote.getAuthor(),  // 받는 사람: 글 작성자
                requester,          // 보낸 사람: 요청자 (조른 사람)
                "TAG_REQUEST",      // 알림 타입
                requester.getName() + "님이 태그를 요청했습니다.",
                quote.getId()       // 클릭 시 이동할 곳
        );
    }
    // 태그 요청 수락
    @Transactional
    public void acceptTagRequest(Long authorId, Long requestId) {
        // 1. 요청 데이터 조회
        QuoteTagRequest request = quoteTagRequestRepository.findById(requestId)
                .orElseThrow(() -> new GlobalException("404", "존재하지 않는 요청입니다."));

        // 2. 권한 검증 (글 작성자 본인만 수락 가능)
        if (!request.getQuote().getAuthor().getId().equals(authorId)) {
            throw new GlobalException("403", "이 요청을 처리할 권한이 없습니다.");
        }

        // 3. 상태 검증 (이미 처리된 요청인지 확인)
        if (request.getStatus() != TagRequestStatus.PENDING) {
            throw new GlobalException("400", "이미 처리된(수락/거절) 요청입니다.");
        }

        // 4. 상태 변경 (PENDING -> ACCEPTED)
        request.accept(); // 엔티티 편의 메서드 사용

        // 5. ★ [핵심] 실제 태그(QuoteTag) 생성 및 저장
        // 요청만 수락하고 태그를 안 만들면 의미가 없죠! 여기서 진짜 태그를 붙여줍니다.
        QuoteTag quoteTag = new QuoteTag(request.getQuote(), request.getRequester());
        quoteTagRepository.save(quoteTag);

        // 6. 알림 발송 (요청자에게 "수락되었습니다" 알림)
        notificationService.create(
                request.getRequester(),         // 받는 사람: 요청했던 친구
                request.getQuote().getAuthor(), // 보낸 사람: 작가(나)
                "TAG_ACCEPTED",                 // 알림 타입 (새로 정의 필요)
                request.getQuote().getAuthor().getName() + "님이 태그 요청을 수락했습니다!",
                request.getQuote().getId()      // 클릭 시 이동할 글 ID
        );
    }

    // 태그 요청 거절
    @Transactional
    public void rejectTagRequest(Long authorId, Long requestId) {
        QuoteTagRequest request = quoteTagRequestRepository.findById(requestId)
                .orElseThrow(() -> new GlobalException("404", "존재하지 않는 요청입니다."));

        if (!request.getQuote().getAuthor().getId().equals(authorId)) {
            throw new GlobalException("403", "이 요청을 처리할 권한이 없습니다.");
        }

        if (request.getStatus() != TagRequestStatus.PENDING) {
            throw new GlobalException("400", "이미 처리된 요청입니다.");
        }

        // 4. 상태 변경 (PENDING -> REJECTED)
        request.reject();

    }

    // [추가] 태그 요청 목록 조회
    public List<QuoteTagRequestResponse> getPendingTagRequests(Long authorId, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new GlobalException("404", "명언을 찾을 수 없습니다."));

        // 작성자 본인만 요청 목록을 볼 수 있음 (보안)
        if (!quote.getAuthor().getId().equals(authorId)) {
            throw new GlobalException("403", "권한이 없습니다.");
        }

        // PENDING(대기) 상태인 요청만 가져오기
        List<QuoteTagRequest> requests = quoteTagRequestRepository.findAllByQuoteIdAndStatus(quoteId, TagRequestStatus.PENDING);

        return requests.stream()
                .map(QuoteTagRequestResponse::from)
                .toList();
    }
}

