package com.ll.demo.domain.quote.quote.service;

import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.group.group.repository.GroupMemberRepository;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.quote.dto.QuoteDetailResponse;
import com.ll.demo.domain.quote.dto.QuoteListDto;
import com.ll.demo.domain.quote.dto.QuoteResponse;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import com.ll.demo.domain.quote.entity.TagRequestStatus;
import com.ll.demo.domain.quote.repository.BookmarkRepository;
import com.ll.demo.domain.quote.repository.QuoteLikeRepository;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRequestRepository;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.gemini.GeminiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock private QuoteRepository quoteRepository;
    @Mock private QuoteLikeRepository quoteLikeRepository;
    @Mock private BookmarkRepository bookmarkRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private QuoteTagRequestRepository quoteTagRequestRepository;
    @Mock private QuoteTagRepository quoteTagRepository;
    @Mock private NotificationService notificationService;
        @Mock private FriendshipRepository friendshipRepository;
        @Mock private GroupMemberRepository groupMemberRepository;
        @Mock private com.ll.demo.domain.group.group.repository.GroupRepository groupRepository;
    @Mock private GeminiService geminiService;

    private Member sender;
    private Quote likedQuote;

    @InjectMocks
    private QuoteService quoteService;

    private Member liker;
    private Member author;
    private Quote testQuote;
    private Member requester;
    private QuoteTagRequest request;

    @BeforeEach
    void setUp() {
        // 현재 로그인 사용자
        sender = Member.builder()
                .nickname("로그인유저")
                .build();
        ReflectionTestUtils.setField(sender, "id", 1L);

        // 좋아요한 사람
        liker = Member.builder()
                .nickname("좋아요요정")
                .build();
        ReflectionTestUtils.setField(liker, "id", 1L);

        // 글 작성자
        author = Member.builder()
                .nickname("작성자")
                .birthYear("1995")
                .build();
        ReflectionTestUtils.setField(author, "id", 2L);

        // 태그 요청한 사람
        requester = Member.builder()
                .nickname("요청자")
                .build();
        ReflectionTestUtils.setField(requester, "id", 3L);

        // 테스트용 명언
        testQuote = Quote.builder()
                .author(author)
                .content("테스트 명언")
                .build();
        ReflectionTestUtils.setField(testQuote, "id", 100L);
        ReflectionTestUtils.setField(testQuote, "createDate", LocalDateTime.now());

        likedQuote = testQuote;

        // 태그해줘
        request = QuoteTagRequest.builder()
                .quote(testQuote)
                .requester(requester)
                .status(TagRequestStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(request, "id", 200L);
    }

    @Test
    @DisplayName("성공: 내가 좋아요를 누른 명언 목록이 정확히 DTO로 변환되어 반환되어야 한다")
    void findLikedQuotes_Success() {
        when(quoteRepository.findQuotesLikedByMember(1L)).thenReturn(List.of(likedQuote));

        List<QuoteResponse> result = quoteService.findLikedQuotes(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(100L);
        assertThat(result.get(0).authorName()).isEqualTo("작성자");

        verify(quoteRepository, times(1)).findQuotesLikedByMember(1L);
    }

    @Test
    @DisplayName("성공: 좋아요를 누르면 데이터가 저장되고 작성자에게 알림이 가야 한다")
    void likeQuote_Success_WithNotification() {
        when(quoteRepository.findById(100L)).thenReturn(Optional.of(testQuote));
        when(quoteLikeRepository.existsByQuoteAndMember(testQuote, liker)).thenReturn(false);

        quoteService.likeQuote(liker, 100L);

        verify(quoteLikeRepository, times(1)).save(any());

        verify(notificationService, times(1)).create(
                eq(author),
                eq(liker),
                eq("LIKE"),
                contains("좋아합니다"),
                eq(100L)
        );
    }

    @Test
    @DisplayName("성공: 수락 시 상태가 ACCEPTED로 변하고 QuoteTag가 생성되어야 한다")
    void acceptTagRequest_Success() {
        Member author = Member.builder().nickname("작성자").build();
        ReflectionTestUtils.setField(author, "id", 1L);

        Member requester = Member.builder().nickname("요청자").build();
        ReflectionTestUtils.setField(requester, "id", 3L);

        Quote quote = Quote.builder().author(author).build();
        ReflectionTestUtils.setField(quote, "id", 10L);

        QuoteTagRequest pendingRequest = QuoteTagRequest.builder()
                .quote(quote)
                .requester(requester)
                .status(TagRequestStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(pendingRequest, "id", 100L);

        when(quoteTagRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

        quoteService.acceptTagRequest(1L, 100L);

        assertThat(pendingRequest.getStatus()).isEqualTo(TagRequestStatus.ACCEPTED);
        verify(quoteTagRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("성공: 특정 날짜 조회 시 내 글과 타인의 글(좋아요/친구 포함)이 정확히 매핑되어야 한다")
    void getQuoteList_Detailed_Success() {
        LocalDate testDate = LocalDate.of(2024, 5, 20);

        Member friend = Member.builder().nickname("친구닉네임").build();
        ReflectionTestUtils.setField(friend, "id", 2L);

        Quote myQuote = Quote.builder().author(sender).content("내 명언").summary("내 요약").build();
        ReflectionTestUtils.setField(myQuote, "id", 10L);
        ReflectionTestUtils.setField(myQuote, "createDate", LocalDateTime.now());

        Quote friendQuote = Quote.builder().author(friend).content("친구 명언").summary("친구 요약").build();
        ReflectionTestUtils.setField(friendQuote, "id", 11L);
        ReflectionTestUtils.setField(friendQuote, "createDate", LocalDateTime.now());

        when(quoteRepository.findAllByDateRange(any(), any())).thenReturn(List.of(myQuote, friendQuote));
        when(quoteLikeRepository.existsByQuoteAndMember(friendQuote, sender)).thenReturn(true);
        when(friendshipRepository.findAllByMember(sender)).thenReturn(List.of(
                com.ll.demo.domain.friendship.friendship.entity.Friendship.builder()
                        .member(sender)
                        .friend(friend)
                        .status(com.ll.demo.domain.friendship.friendship.type.FriendshipStatus.ACCEPTED)
                        .build()
        ));
        when(groupMemberRepository.findByMember(sender)).thenReturn(List.of()); // 그룹 없음 처리
        when(bookmarkRepository.existsByMemberAndQuote(sender, myQuote)).thenReturn(false);
        when(bookmarkRepository.existsByMemberAndQuote(sender, friendQuote)).thenReturn(false);

        QuoteListDto result = quoteService.getQuoteList(sender, testDate, null);

        assertThat(result.myQuotes()).hasSize(1);
        assertThat(result.myQuotes().get(0).id()).isEqualTo(10L);

        assertThat(result.otherQuotes()).hasSize(1);
        QuoteDetailResponse other = result.otherQuotes().get(0);
        assertThat(other.isLiked()).isTrue();
        assertThat(other.isFriendQuote()).isTrue();
        assertThat(other.content()).isEqualTo("친구 요약");
        // groupId == null 인 경우 unwrittenMembers는 빈 리스트여야 한다
        assertThat(result.unwrittenMembers()).isEmpty();
    }

        @Test
        @DisplayName("unwrittenMembers: 그룹 일부만 작성한 경우 미작성자만 반환해야 한다")
        void getQuoteList_UnwrittenMembers_PartialGroup() {
                LocalDate testDate = LocalDate.of(2024, 5, 20);

                // 그룹, 멤버 생성
                com.ll.demo.domain.group.group.entity.Group group = com.ll.demo.domain.group.group.entity.Group.builder().name("g").build();
                ReflectionTestUtils.setField(group, "id", 10L);

                Member memberA = Member.builder().nickname("A").build(); ReflectionTestUtils.setField(memberA, "id", 1L); // current user (sender)
                Member memberB = Member.builder().nickname("B").build(); ReflectionTestUtils.setField(memberB, "id", 2L); // wrote
                Member memberC = Member.builder().nickname("C").build(); ReflectionTestUtils.setField(memberC, "id", 3L); // unwritten

                // group members include sender, B, C
                com.ll.demo.domain.group.group.entity.GroupMember gmA = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(memberA).build();
                com.ll.demo.domain.group.group.entity.GroupMember gmB = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(memberB).build();
                com.ll.demo.domain.group.group.entity.GroupMember gmC = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(memberC).build();

                when(groupRepository.findById(10L)).thenReturn(java.util.Optional.of(group));
                when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(gmA, gmB, gmC));

                // quotes: only memberB wrote
                Quote quoteB = Quote.builder().author(memberB).content("b").summary("b").build(); ReflectionTestUtils.setField(quoteB, "id", 100L); ReflectionTestUtils.setField(quoteB, "createDate", LocalDateTime.now());
                when(quoteRepository.findAllByDateRange(any(), any())).thenReturn(List.of(quoteB));
                when(bookmarkRepository.existsByMemberAndQuote(memberA, quoteB)).thenReturn(false);
                when(quoteLikeRepository.existsByQuoteAndMember(quoteB, memberA)).thenReturn(false);
                when(quoteTagRepository.findAllByQuote(quoteB)).thenReturn(List.of());

                // call as currentUser = memberA
                QuoteListDto result = quoteService.getQuoteList(memberA, testDate, 10L);

                // unwritten should contain memberC only (memberA excluded)
                assertThat(result.unwrittenMembers()).hasSize(1);
                assertThat(result.unwrittenMembers().get(0).memberId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("unwrittenMembers: groupId null이면 빈리스트 반환")
        void getQuoteList_UnwrittenMembers_GroupIdNull() {
                LocalDate testDate = LocalDate.of(2024, 5, 20);

                when(quoteRepository.findAllByDateRange(any(), any())).thenReturn(List.of());

                QuoteListDto result = quoteService.getQuoteList(sender, testDate, null);

                assertThat(result.unwrittenMembers()).isEmpty();
        }

        @Test
        @DisplayName("unwrittenMembers: 그룹의 모든 대상자가 작성한 경우 빈 리스트")
        void getQuoteList_AllWrote_ShouldBeEmpty() {
                LocalDate testDate = LocalDate.of(2024, 5, 20);

                com.ll.demo.domain.group.group.entity.Group group = com.ll.demo.domain.group.group.entity.Group.builder().name("g").build();
                ReflectionTestUtils.setField(group, "id", 20L);

                Member a = Member.builder().nickname("A").build(); ReflectionTestUtils.setField(a, "id", 1L);
                Member b = Member.builder().nickname("B").build(); ReflectionTestUtils.setField(b, "id", 2L);
                Member c = Member.builder().nickname("C").build(); ReflectionTestUtils.setField(c, "id", 3L);

                com.ll.demo.domain.group.group.entity.GroupMember gmA = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(a).build();
                com.ll.demo.domain.group.group.entity.GroupMember gmB = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(b).build();
                com.ll.demo.domain.group.group.entity.GroupMember gmC = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(c).build();

                when(groupRepository.findById(20L)).thenReturn(java.util.Optional.of(group));
                when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(gmA, gmB, gmC));

                Quote qa = Quote.builder().author(a).content("a").build(); ReflectionTestUtils.setField(qa, "id", 201L); ReflectionTestUtils.setField(qa, "createDate", LocalDateTime.now());
                Quote qb = Quote.builder().author(b).content("b").build(); ReflectionTestUtils.setField(qb, "id", 202L); ReflectionTestUtils.setField(qb, "createDate", LocalDateTime.now());
                Quote qc = Quote.builder().author(c).content("c").build(); ReflectionTestUtils.setField(qc, "id", 203L); ReflectionTestUtils.setField(qc, "createDate", LocalDateTime.now());
                when(quoteRepository.findAllByDateRange(any(), any())).thenReturn(List.of(qa, qb, qc));

                QuoteListDto result = quoteService.getQuoteList(a, testDate, 20L);
                assertThat(result.unwrittenMembers()).isEmpty();
        }

        @Test
        @DisplayName("unwrittenMembers: 아무도 작성하지 않은 경우 현재 사용자 제외한 모두 반환")
        void getQuoteList_NoneWrote_AllUnwrittenExceptCurrent() {
                LocalDate testDate = LocalDate.of(2024, 5, 20);

                com.ll.demo.domain.group.group.entity.Group group = com.ll.demo.domain.group.group.entity.Group.builder().name("g").build();
                ReflectionTestUtils.setField(group, "id", 30L);

                Member a = Member.builder().nickname("A").build(); ReflectionTestUtils.setField(a, "id", 1L);
                Member b = Member.builder().nickname("B").build(); ReflectionTestUtils.setField(b, "id", 2L);
                Member c = Member.builder().nickname("C").build(); ReflectionTestUtils.setField(c, "id", 3L);

                com.ll.demo.domain.group.group.entity.GroupMember gmA = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(a).build();
                com.ll.demo.domain.group.group.entity.GroupMember gmB = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(b).build();
                com.ll.demo.domain.group.group.entity.GroupMember gmC = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(c).build();

                when(groupRepository.findById(30L)).thenReturn(java.util.Optional.of(group));
                when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(gmA, gmB, gmC));

                when(quoteRepository.findAllByDateRange(any(), any())).thenReturn(List.of());

                QuoteListDto result = quoteService.getQuoteList(a, testDate, 30L);
                assertThat(result.unwrittenMembers()).hasSize(2);
                List<Long> ids = result.unwrittenMembers().stream().map(x -> x.memberId()).toList();
                assertThat(ids).containsExactlyInAnyOrder(2L, 3L);
        }

        @Test
        @DisplayName("unwrittenMembers: 한 멤버가 같은 날짜 여러 Quote를 가질 경우 작성자로 판단")
        void getQuoteList_MultipleQuotesPerAuthor() {
                LocalDate testDate = LocalDate.of(2024, 5, 20);

                com.ll.demo.domain.group.group.entity.Group group = com.ll.demo.domain.group.group.entity.Group.builder().name("g").build();
                ReflectionTestUtils.setField(group, "id", 40L);

                Member a = Member.builder().nickname("A").build(); ReflectionTestUtils.setField(a, "id", 1L);
                Member b = Member.builder().nickname("B").build(); ReflectionTestUtils.setField(b, "id", 2L);

                com.ll.demo.domain.group.group.entity.GroupMember gmA = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(a).build();
                com.ll.demo.domain.group.group.entity.GroupMember gmB = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(b).build();

                when(groupRepository.findById(40L)).thenReturn(java.util.Optional.of(group));
                when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(gmA, gmB));

                Quote q1 = Quote.builder().author(b).content("b1").build(); ReflectionTestUtils.setField(q1, "id", 301L); ReflectionTestUtils.setField(q1, "createDate", LocalDateTime.now());
                Quote q2 = Quote.builder().author(b).content("b2").build(); ReflectionTestUtils.setField(q2, "id", 302L); ReflectionTestUtils.setField(q2, "createDate", LocalDateTime.now());
                when(quoteRepository.findAllByDateRange(any(), any())).thenReturn(List.of(q1, q2));

                QuoteListDto result = quoteService.getQuoteList(a, testDate, 40L);
                // b wrote multiple quotes; since b is the only target member (a is current user and excluded), unwrittenMembers should be empty
                assertThat(result.unwrittenMembers()).isEmpty();
        }

        @Test
        @DisplayName("date range passed to repository matches requested date boundaries")
        void getQuoteList_DateRangeArguments() {
                LocalDate testDate = LocalDate.of(2026, 8, 29);

                // call
                when(quoteRepository.findAllByDateRange(any(), any())).thenReturn(List.of());

                quoteService.getQuoteList(sender, testDate, null);

                ArgumentCaptor<java.time.LocalDateTime> captor1 = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
                ArgumentCaptor<java.time.LocalDateTime> captor2 = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
                verify(quoteRepository, times(1)).findAllByDateRange(captor1.capture(), captor2.capture());

                java.time.LocalDateTime start = captor1.getValue();
                java.time.LocalDateTime end = captor2.getValue();

                assertThat(start).isEqualTo(testDate.atStartOfDay());
                assertThat(end).isEqualTo(testDate.plusDays(1).atStartOfDay());
        }

        @Test
        @DisplayName("존재하지 않는 groupId는 404 오류를 던진다")
        void getQuoteList_NonexistentGroup_Throws404() {
                LocalDate testDate = LocalDate.of(2024, 5, 20);
                when(groupRepository.findById(999999L)).thenReturn(Optional.empty());

                assertThrows(com.ll.demo.global.exceptions.GlobalException.class, () -> {
                        quoteService.getQuoteList(sender, testDate, 999999L);
                });
        }

        @Test
        @DisplayName("비회원이 groupId로 접근해도 현재 구현은 에러를 던지지 않음(멤버 체크 없음)")
        void getQuoteList_NonMemberAccess_AllowsAccessByDesign() {
                LocalDate testDate = LocalDate.of(2024, 5, 20);

                com.ll.demo.domain.group.group.entity.Group group = com.ll.demo.domain.group.group.entity.Group.builder().name("g").build();
                ReflectionTestUtils.setField(group, "id", 50L);

                Member b = Member.builder().nickname("B").build(); ReflectionTestUtils.setField(b, "id", 2L);
                Member c = Member.builder().nickname("C").build(); ReflectionTestUtils.setField(c, "id", 3L);

                com.ll.demo.domain.group.group.entity.GroupMember gmB = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(b).build();
                com.ll.demo.domain.group.group.entity.GroupMember gmC = com.ll.demo.domain.group.group.entity.GroupMember.builder().group(group).member(c).build();

                when(groupRepository.findById(50L)).thenReturn(Optional.of(group));
                when(groupMemberRepository.findByGroup(group)).thenReturn(List.of(gmB, gmC));
                when(quoteRepository.findAllByDateRange(any(), any())).thenReturn(List.of());

                // sender (id=1) is not in group members; service should still return result without throwing
                QuoteListDto result = quoteService.getQuoteList(sender, testDate, 50L);
                assertThat(result.unwrittenMembers()).hasSize(2);
        }
}