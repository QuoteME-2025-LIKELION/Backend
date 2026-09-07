package com.ll.demo.domain.quote.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.group.group.repository.GroupRepository;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.quote.dto.*;
import com.ll.demo.domain.quote.entity.*;
import com.ll.demo.domain.quote.repository.*;
import com.ll.demo.global.dto.PagedResponse;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
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
    private final GroupRepository groupRepository;
    private final BookmarkRepository bookmarkRepository;
    //  입력 검증

    /** 검증: 30자 초과 불가, 불량 텍스트 불가 */
    private void validateQuoteContent(String content) {
        if (content == null || content.isBlank()) {
            throw new GlobalException("400-1", "내용을 입력해주세요.");
        }
        if (content.length() > 30) {
            throw new GlobalException("400-2", "30자를 초과한 글은 게시할 수 없습니다.");
        }
        if (isLowQualityText(content)) {
            throw new GlobalException("400-3", "초성이나 특수문자로만 이루어진 글은 게시할 수 없습니다.");
        }
    }

    /** 15자 이하 경고, 불량 텍스트 불가 */
    public void validateDiaryContent(String content) {
        if (content == null || content.isBlank()) {
            throw new GlobalException("400-1", "내용을 입력해주세요.");
        }
        if (isLowQualityText(content)) {
            throw new GlobalException("400-3", "초성이나 특수문자로만 이루어진 글은 게시할 수 없습니다.");
        }
        if (content.trim().length() <= 15) {
            throw new GlobalException("400-4", "15자 이하의 글은 양질의 명언을 만들기 어려워요");
        }
    }

    /**
     * 저품질 텍스트 판별:
     *  - 한국어 자음(ㄱ~ㅎ)으로만 구성된 경우
     *  - 특수문자/공백으로만 구성된 경우
     */
    private boolean isLowQualityText(String text) {
        String noSpace = text.replaceAll("\\s", "");
        if (noSpace.isBlank()) return true;

        // 한국어 자음만
        boolean onlyKoreanConsonants = noSpace.chars().allMatch(c -> c >= 0x3131 && c <= 0x314E);
        if (onlyKoreanConsonants) return true;

        // 글자 또는 숫자가 하나도 없으면 특수문자만
        boolean onlySpecialChars = noSpace.chars().noneMatch(Character::isLetterOrDigit);
        return onlySpecialChars;
    }

    //  명언 작성

    @Transactional
    public QuoteResponse createQuote(Long authorId, String content, String originalContent, String summary, List<Long> taggedMemberIds) {
        validateQuoteContent(content);
        validateOneQuotePerDay(authorId);

        Member author = memberRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        Quote quote = Quote.builder()
                .author(author)
                .content(content)
                .originalContent(originalContent)
                .summary(summary)
                .build();

        quoteRepository.save(quote);

        if (taggedMemberIds != null && !taggedMemberIds.isEmpty()) {
            for (Long memberId : taggedMemberIds.stream().distinct().toList()) {
                Member taggedMember = memberRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("태그된 회원을 찾을 수 없습니다."));

                quoteTagRepository.save(new QuoteTag(quote, taggedMember));

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

    //  AI 사용량 정보

    public Map<String, Object> getAiUsageInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();
        int used = 0;
        if (today.equals(member.getAiUsageDate())) {
            used = member.getAiUsageCount();
        }
        int remaining = Math.max(0, 3 - used);
        return Map.of("used", used, "remaining", remaining, "limit", 3);
    }

    //  좋아요

    @Transactional
    public void likeQuote(Member member, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        if (quoteLikeRepository.existsByQuoteAndMember(quote, member)) {
            return;
        }

        quoteLikeRepository.save(new QuoteLike(quote, member));

        if (!quote.getAuthor().getId().equals(member.getId())) {
            notificationService.create(
                    quote.getAuthor(),
                    member,
                    "LIKE",
                    member.getName() + "님이 당신의 명언을 좋아합니다.",
                    quote.getId()
            );
        }
    }

    @Transactional
    public void unlikeQuote(Member member, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        quoteLikeRepository.findByQuoteAndMember(quote, member)
                .ifPresent(quoteLikeRepository::delete);
    }

    public QuoteListDto getQuoteList(Member currentUser, LocalDate date, Long groupId) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.plusDays(1).atStartOfDay();

        List<Quote> quotes = quoteRepository.findAllByDateRange(startDate, endDate);

        // 내 명언
        List<MyQuoteResponse> myQuotes = quotes.stream()
                .filter(q -> q.getAuthor().getId().equals(currentUser.getId()))
                .map(q -> MyQuoteResponse.from(q, getQuoteGroupName(q)))
                .toList();

        // 다른 사람 명언 (그룹 필터 or 친구 필터)
        List<Member> targetMembers;
        if (groupId != null) {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new GlobalException("404", "그룹을 찾을 수 없습니다."));
            targetMembers = groupMemberRepository.findByGroup(group).stream()
                    .map(GroupMember::getMember)
                    .filter(m -> !m.getId().equals(currentUser.getId()))
                    .toList();
        } else {
            targetMembers = friendshipRepository.findAllByMember(currentUser).stream()
                    .map(Friendship::getFriend)
                    .toList();
        }

        List<QuoteDetailResponse> otherQuotes = quotes.stream()
                .filter(q -> !q.getAuthor().getId().equals(currentUser.getId()))
                .filter(q -> targetMembers.contains(q.getAuthor()))
                .map(q -> {
                    boolean isLiked = quoteLikeRepository.existsByQuoteAndMember(q, currentUser);
                    boolean isBookmarked = bookmarkRepository.existsByMemberAndQuote(currentUser, q);
                    List<String> taggedNicknames = quoteTagRepository.findAllByQuote(q).stream()
                            .map(qt -> qt.getMember().getNickname())
                            .toList();
                    return QuoteDetailResponse.from(q, taggedNicknames, isLiked, isBookmarked, true);
                })
                .toList();

        // compute unwritten members only when groupId is provided
        List<UnwrittenMemberResponse> unwrittenMembers = Collections.emptyList();
        if (groupId != null) {
            // authors who have at least one quote in the date range
            Set<Long> authorsWithQuote = quotes.stream()
                .map(q -> q.getAuthor().getId())
                .collect(Collectors.toSet());

            unwrittenMembers = targetMembers.stream()
                .filter(m -> !authorsWithQuote.contains(m.getId()))
                .filter(m -> !m.getId().equals(currentUser.getId()))
                .map(m -> new com.ll.demo.domain.quote.dto.UnwrittenMemberResponse(m.getId(), m.getNickname(), m.getProfileImage()))
                .toList();
        }

        return new QuoteListDto(myQuotes, otherQuotes, unwrittenMembers);
    }

    private String getQuoteGroupName(Quote quote) {
        List<GroupMember> groupMembers = groupMemberRepository.findByMember(quote.getAuthor());
        if (!groupMembers.isEmpty()) {
            return groupMembers.get(0).getGroup().getName();
        }
        return "아직 그룹이 없습니다.";
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        if (authentication.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getMember().getId();
        }
        return null;
    }

    // 아카이브 조회

    public List<QuoteResponse> findMyQuotes(Long memberId) {
        return quoteRepository.findAllByAuthorId(memberId).stream()
                .map(QuoteResponse::new)
                .collect(Collectors.toList());
    }

    public List<QuoteResponse> findLikedQuotes(Long memberId) {
        return quoteRepository.findQuotesLikedByMember(memberId).stream()
                .map(QuoteResponse::from)
                .collect(Collectors.toList());
    }

    public List<QuoteResponse> findMyQuotesByDate(Long memberId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return quoteRepository.findAllByAuthorIdAndCreateDateBetweenOrderByCreateDateDesc(memberId, startOfDay, endOfDay)
                .stream()
                .map(QuoteResponse::new)
                .toList();
    }

    // 북마크

    @Transactional
    public void bookmarkQuote(Long memberId, Long quoteId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new GlobalException("404", "명언을 찾을 수 없습니다."));

        if (bookmarkRepository.existsByMemberAndQuote(member, quote)) {
            throw new GlobalException("400-1", "이미 북마크한 명언입니다.");
        }

        bookmarkRepository.save(Bookmark.builder().member(member).quote(quote).build());
    }

    @Transactional
    public void unbookmarkQuote(Long memberId, Long quoteId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new GlobalException("404", "명언을 찾을 수 없습니다."));

        bookmarkRepository.findByMemberAndQuote(member, quote)
                .ifPresentOrElse(bookmarkRepository::delete, () -> {
                    throw new GlobalException("404", "북마크 정보를 찾을 수 없습니다.");
                });
    }

    public List<QuoteResponse> findBookmarkedQuotes(Long memberId) {
        return bookmarkRepository.findQuotesByMemberId(memberId).stream()
                .map(QuoteResponse::from)
                .collect(Collectors.toList());
    }

    // 태그

    @Transactional
    public void updateTags(Long authorId, Long quoteId, List<Long> taggedMemberIds) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        if (!quote.getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("수정 권한이 없습니다.");
        }

        quoteTagRepository.deleteAllByQuote(quote);

        if (taggedMemberIds != null && !taggedMemberIds.isEmpty()) {
            for (Long memberId : taggedMemberIds) {
                Member taggedMember = memberRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("존재하지 않는 회원입니다."));

                quoteTagRepository.save(new QuoteTag(quote, taggedMember));

                notificationService.create(
                        taggedMember,
                        quote.getAuthor(),
                        "TAG",
                        quote.getAuthor().getName() + "님이 글에 태그했습니다.",
                        quote.getId()
                );
            }
        }
    }

    @Transactional
    public void requestTag(Long requesterId, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("명언을 찾을 수 없습니다."));

        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (quote.getAuthor().getId().equals(requesterId)) {
            throw new RuntimeException("본인 글에는 태그 요청을 할 수 없습니다.");
        }

        if (quoteTagRequestRepository.existsByQuoteAndRequester(quote, requester)) {
            throw new GlobalException("400-1", "이미 태그 요청을 보냈습니다.");
        }

        QuoteTagRequest request = QuoteTagRequest.builder()
                .quote(quote)
                .requester(requester)
                .status(TagRequestStatus.PENDING)
                .build();

        quoteTagRequestRepository.save(request);

        notificationService.create(
                quote.getAuthor(),
                requester,
                "TAG_REQUEST",
                requester.getName() + "님이 태그를 요청했습니다.",
                quote.getId()
        );
    }

    /** 내 태그 요청 상태 조회 (PENDING / ACCEPTED / REJECTED / NONE) */
    public String getMyTagRequestStatus(Long requesterId, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new GlobalException("404", "명언을 찾을 수 없습니다."));
        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));

        return quoteTagRequestRepository.findByQuoteAndRequester(quote, requester)
                .map(req -> req.getStatus().name())
                .orElse("NONE");
    }

    @Transactional
    public void acceptTagRequest(Long authorId, Long requestId) {
        QuoteTagRequest request = quoteTagRequestRepository.findById(requestId)
                .orElseThrow(() -> new GlobalException("404", "존재하지 않는 요청입니다."));

        if (!request.getQuote().getAuthor().getId().equals(authorId)) {
            throw new GlobalException("403", "이 요청을 처리할 권한이 없습니다.");
        }

        if (request.getStatus() != TagRequestStatus.PENDING) {
            throw new GlobalException("400", "이미 처리된(수락/거절) 요청입니다.");
        }

        if (quoteTagRepository.existsByQuoteAndMember(request.getQuote(), request.getRequester())) {
            throw new GlobalException("400", "이미 태그된 사용자입니다.");
        }

        request.accept();

        quoteTagRepository.save(new QuoteTag(request.getQuote(), request.getRequester()));

        notificationService.create(
                request.getRequester(),
                request.getQuote().getAuthor(),
                "TAG_ACCEPTED",
                request.getQuote().getAuthor().getName() + "님이 태그 요청을 수락했습니다!",
                request.getQuote().getId()
        );
    }

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

        request.reject();
    }

    public List<QuoteTagRequestResponse> getPendingTagRequests(Long authorId, Long quoteId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new GlobalException("404", "명언을 찾을 수 없습니다."));

        if (!quote.getAuthor().getId().equals(authorId)) {
            throw new GlobalException("403", "권한이 없습니다.");
        }

        return quoteTagRequestRepository.findAllByQuoteIdAndStatus(quoteId, TagRequestStatus.PENDING).stream()
                .map(QuoteTagRequestResponse::from)
                .collect(Collectors.toList());
    }

    // 콕 찌르기

    @Transactional
    public void poke(Member sender, Long receiverId) {
        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("대상 회원을 찾을 수 없습니다."));

        if (sender.getId().equals(receiverId)) {
            throw new RuntimeException("자기 자신은 콕 찌를 수 없습니다.");
        }

        notificationService.create(
                receiver,
                sender,
                "POKE",
                sender.getNickname() + "님이 당신을 콕 찔렀습니다!",
                null
        );
    }

    public long getPokeCount(Long memberId) {
        return notificationService.countUnreadByType(memberId, "POKE");
    }

    public PagedResponse<QuoteDetailResponse> getFeed(Long memberId, LocalDate date, Long groupId) {
        List<Long> friendIds = friendshipRepository.findFriendIds(memberId);

        if (friendIds.isEmpty()) {
            return PagedResponse.empty("친구를 추가하면 매일 피드에서 명언을 확인할 수 있어요");
        }

        List<Quote> quotes = quoteRepository.findFeedQuotes(friendIds, date, groupId);

        if (quotes.isEmpty()) {
            return PagedResponse.empty("아직 아무도 명언을 작성하지 않았어요");
        }

        Member currentUser = memberRepository.findById(memberId)
                .orElseThrow(() -> new GlobalException("404", "회원을 찾을 수 없습니다."));

        List<QuoteDetailResponse> result = quotes.stream()
                .map(q -> {
                    boolean isLiked = quoteLikeRepository.existsByQuoteAndMember(q, currentUser);
                    boolean isBookmarked = bookmarkRepository.existsByMemberAndQuote(currentUser, q);
                    List<String> taggedNicknames = quoteTagRepository.findAllByQuote(q).stream()
                            .map(qt -> qt.getMember().getNickname())
                            .toList();
                    return QuoteDetailResponse.from(q, taggedNicknames, isLiked, isBookmarked, true);
                })
                .toList();

        return PagedResponse.of(result);
    }
}
