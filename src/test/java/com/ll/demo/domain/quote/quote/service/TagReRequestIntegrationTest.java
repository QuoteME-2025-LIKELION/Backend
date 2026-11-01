package com.ll.demo.domain.quote.quote.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import com.ll.demo.domain.quote.entity.TagRequestStatus;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRequestRepository;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.exceptions.GlobalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TagReRequestIntegrationTest {

    @Autowired private QuoteService quoteService;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private QuoteTagRequestRepository quoteTagRequestRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private NotificationRepository notificationRepository;

    private Member author;
    private Member requester;
    private Quote quote;

    @BeforeEach
    void setUp() {
        author = memberRepository.save(Member.builder().email("a@test.com").nickname("작성자").password("password123").build());
        requester = memberRepository.save(Member.builder().email("r@test.com").nickname("요청자").password("password123").build());
        friendshipRepository.save(Friendship.builder().member(requester).friend(author).status(FriendshipStatus.ACCEPTED).build());
        quote = quoteRepository.save(Quote.builder().author(author).content("명언").originalContent("원문").build());
    }

    private QuoteTagRequest request() {
        return quoteTagRequestRepository.findByQuoteAndRequester(quote, requester).orElseThrow();
    }

    private String myTagStatusInFeed() {
        return quoteService.getFeed(requester.getId(), LocalDate.now(), null).getItems().get(0).myTagStatus();
    }

    private long tagRequestNotifications() {
        return notificationRepository.findAll().stream().filter(n -> n.getType().equals("TAG_REQUEST")).count();
    }

    @Test
    @DisplayName("거절 후 재요청: 같은 요청이 PENDING으로 돌아가고 알림을 다시 보낸다")
    void rejectedThenReRequest() {
        quoteService.requestTag(requester.getId(), quote.getId());
        assertThat(myTagStatusInFeed()).isEqualTo("REQUESTED");

        request().reject();
        assertThat(myTagStatusInFeed()).isEqualTo("NONE");
        assertThat(quoteService.getMyTagRequestStatus(requester.getId(), quote.getId())).isEqualTo("REJECTED");

        quoteService.requestTag(requester.getId(), quote.getId());

        assertThat(request().getStatus()).isEqualTo(TagRequestStatus.PENDING);
        assertThat(quoteTagRequestRepository.count()).isEqualTo(1);
        assertThat(tagRequestNotifications()).isEqualTo(2);
        assertThat(myTagStatusInFeed()).isEqualTo("REQUESTED");
    }

    @Test
    @DisplayName("대기 중이면 재요청은 400 유지")
    void pending_400() {
        quoteService.requestTag(requester.getId(), quote.getId());

        assertThatThrownBy(() -> quoteService.requestTag(requester.getId(), quote.getId()))
                .isInstanceOf(GlobalException.class)
                .hasMessageContaining("400-1");
    }
}
