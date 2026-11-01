package com.ll.demo.domain.quote.quote.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteTag;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRepository;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.exceptions.GlobalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TagRulesIntegrationTest {

    @Autowired private QuoteService quoteService;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private QuoteTagRepository quoteTagRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberService memberService;
    @Autowired private FriendshipRepository friendshipRepository;

    private Member author;
    private Member friend;
    private Member stranger;
    private Member pendingFriend;

    @BeforeEach
    void setUp() {
        author = member("author");
        friend = member("friend");
        stranger = member("stranger");
        pendingFriend = member("pending");
        friendship(author, friend, FriendshipStatus.ACCEPTED);
        friendship(author, pendingFriend, FriendshipStatus.REQUESTED);
    }

    private Member member(String name) {
        return memberRepository.save(Member.builder().email(name + "@test.com").nickname(name).password("password123").build());
    }

    private void friendship(Member from, Member to, FriendshipStatus status) {
        friendshipRepository.save(Friendship.builder().member(from).friend(to).status(status).build());
    }

    private void assertBadRequest(Executable executable) {
        GlobalException ex = assertThrows(GlobalException.class, executable);
        assertThat(ex.getRsData().getStatusCode()).isEqualTo(400);
    }

    private Quote existingQuote() {
        return quoteRepository.save(Quote.builder().author(author).content("명언").originalContent("원문").build());
    }

    private void create(Long... tagged) {
        quoteService.createQuote(author.getId(), "오늘의 명언", "원문", null, List.of(tagged));
    }

    @Test
    @DisplayName("작성 시 수락된 친구는 태그할 수 있다")
    void create_friend_ok() {
        create(friend.getId());

        assertThat(quoteTagRepository.findAll()).extracting(tag -> tag.getMember().getId()).contains(friend.getId());
    }

    @Test
    @DisplayName("작성 시 친구가 아니면 400")
    void create_stranger_400() {
        assertBadRequest(() -> create(stranger.getId()));
    }

    @Test
    @DisplayName("작성 시 수락 대기 중인 친구면 400")
    void create_pendingFriend_400() {
        assertBadRequest(() -> create(pendingFriend.getId()));
    }

    @Test
    @DisplayName("작성 시 본인이면 400")
    void create_self_400() {
        assertBadRequest(() -> create(author.getId()));
    }

    @Test
    @DisplayName("수정 시 새로 추가하는 대상도 같은 규칙")
    void update_invalidTargets_400() {
        Quote quote = existingQuote();

        assertBadRequest(() -> quoteService.updateTags(author.getId(), quote.getId(), List.of(stranger.getId())));
        assertBadRequest(() -> quoteService.updateTags(author.getId(), quote.getId(), List.of(author.getId())));
    }

    @Test
    @DisplayName("수정 시 이미 태그돼 있던 비친구(수락된 태그 요청)는 유지할 수 있다")
    void update_keepsExistingTag() {
        Quote quote = existingQuote();
        quoteTagRepository.save(new QuoteTag(quote, stranger));

        quoteService.updateTags(author.getId(), quote.getId(), List.of(stranger.getId(), friend.getId()));

        assertThat(quoteTagRepository.findAllByQuote(quote)).hasSize(2);
    }

    @Test
    @DisplayName("게스트 시드의 태그는 작성자 본인이 아니고 수락된 친구다")
    void guestSeed_satisfiesTagRule() {
        memberService.findOrCreateGuest();

        List<QuoteTag> tags = quoteTagRepository.findAll();

        assertThat(tags).isNotEmpty().allSatisfy(tag -> {
            Member quoteAuthor = tag.getQuote().getAuthor();
            assertThat(tag.getMember().getId()).isNotEqualTo(quoteAuthor.getId());
            assertThat(friendshipRepository.existsByMemberAndFriendAndStatus(quoteAuthor, tag.getMember(), FriendshipStatus.ACCEPTED))
                    .as("%s → %s", quoteAuthor.getNickname(), tag.getMember().getNickname())
                    .isTrue();
        });
    }
}
