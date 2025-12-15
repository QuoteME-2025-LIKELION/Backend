package com.ll.demo.domain.quote.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
import com.ll.demo.domain.quote.entity.QuoteTag;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRequestRepository;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.security.SecurityUser;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.quote.dto.MyQuoteResponse;
import com.ll.demo.domain.quote.dto.QuoteDetailResponse;
import com.ll.demo.domain.quote.dto.QuoteListDto;
import com.ll.demo.domain.quote.dto.QuoteResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteLikeRepository quoteLikeRepository;
    private final MemberRepository memberRepository;
    private final QuoteTagRequestRepository quoteTagRequestRepository;
    private final QuoteTagRepository quoteTagRepository;
    private final NotificationService notificationService;
    private final FriendshipRepository friendshipRepository;
    private final GroupMemberRepository groupMemberRepository;

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

    // 명언 목록 조회 - 날짜 필터링+필요정보
    public QuoteListDto getQuoteList(Member currentUser, LocalDate date) {

        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.plusDays(1).atStartOfDay();

        List<Quote> quotes = quoteRepository.findAllByDateRange(startDate, endDate);
        List<MyQuoteResponse> myQuotes = quotes.stream()
                .filter(q -> q.getAuthor().getId().equals(currentUser.getId()))
                .map(q -> {
                    String groupName = getQuoteGroupName(q);
                    return MyQuoteResponse.from(q, groupName);
                })
                .toList();
        List<QuoteDetailResponse> otherQuotes = quotes.stream()
                .filter(q -> !q.getAuthor().getId().equals(currentUser.getId()))
                .map(q -> {
                    boolean isLiked = quoteLikeRepository.existsByQuoteAndMember(q, currentUser);
                    boolean isFriend = friendshipRepository.existsByMemberAndFriend(currentUser, q.getAuthor());
                    List<String> taggedNicknames = quoteTagRepository.findAllByQuote(q).stream()
                            .map(qt -> qt.getMember().getNickname()) // 🟢 QuoteTag의 getMember() 호출
                            // 🟢 ERROR: List<Object> -> List<String> 해결. .toList()는 Java 16 이상에서 타입 추론 가능
                            .collect(Collectors.toList()); // 🟢 명시적 collect로 타입 오류 회피
                    return QuoteDetailResponse.from(q, taggedNicknames, isLiked, isFriend);
                })
                .toList();

        return new QuoteListDto(myQuotes, otherQuotes);
    }

    // 명언 작성자가 속한 그룹
    private String getQuoteGroupName(Quote quote) {
        List<GroupMember> groupMembers = groupMemberRepository.findByMember(quote.getAuthor());
        if (!groupMembers.isEmpty()) {
            return groupMembers.get(0).getGroup().getName();
        }
        return "아직 그룹이 없습니다.";
    }

    // 로그인한 사용자
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null; // 인증되지 않은 사용자
        }

        if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getMember().getId();
        }

        return null;
    }

    // 명언에 태그 요청하는 메서드
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

    // 🟢 1. 내가 작성한 명언 목록 조회 (ArchiveController에서 호출)
    public List<QuoteResponse> findMyQuotes(Long memberId) {
        // [로직] memberId를 기준으로 작성된 모든 명언을 조회합니다.
        List<Quote> myQuotes = quoteRepository.findAllByAuthorId(memberId);

        return myQuotes.stream()
                .map(QuoteResponse::from) // QuoteResponse.from(Quote) 메서드가 있다고 가정
                .collect(Collectors.toList());
    }

    // 🟢 2. 내가 좋아요 누른 명언 목록 조회 (ArchiveController에서 호출)
    public List<QuoteResponse> findLikedQuotes(Long memberId) {
        // [로직] QuoteLike 엔티티를 조인하거나, 별도의 쿼리를 통해 memberId가 좋아요를 누른 명언만 조회합니다.
        // QuoteRepository에 @Query를 사용한 메서드가 필요합니다. (아래 1단계 참고)
        List<Quote> likedQuotes = quoteRepository.findQuotesLikedByMember(memberId);

        return likedQuotes.stream()
                .map(QuoteResponse::from) // QuoteResponse.from(Quote) 메서드가 있다고 가정
                .collect(Collectors.toList());
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

//    // [추가] 좋아요한 글 목록 조회
//    public List<QuoteResponse> findLikedQuotes(Long memberId) {
//        // 1. DB에서 내가 좋아요한 Quote 목록 조회
//        List<Quote> likedQuotes = quoteRepository.findQuotesLikedByMember(memberId);
//
//        // 2. DTO로 변환
//        return likedQuotes.stream()
//                .map(QuoteResponse::new)
//                .toList();
//    }

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
}