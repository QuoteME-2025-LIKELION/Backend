package com.ll.demo.domain.quote.quote.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.Bookmark;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
import com.ll.demo.domain.quote.entity.QuoteTag;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import com.ll.demo.domain.quote.entity.TagRequestStatus;
import com.ll.demo.domain.quote.service.QuoteService;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 피드 쿼리를 바꿔도 응답이 같음을 고정하고, 명언 수가 늘어도 쿼리 수가 늘지 않음을 검증
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FeedResponseIdentityTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;
    @Autowired private QuoteService quoteService;
    @Autowired private AuthTokenService authTokenService;

    private Member viewer;
    private Member friendA;
    private Member friendB;
    private Quote tagged;
    private Quote requested;
    private Quote rejected;
    private Quote mine;

    @BeforeEach
    void setUp() {
        viewer = member("viewer");
        friendA = member("friendA");
        friendB = member("friendB");
        Member unwritten = member("unwritten");
        Member stranger = member("stranger");
        friends(viewer, friendA);
        friends(viewer, friendB);
        friends(viewer, unwritten);

        tagged = quote(friendA, "태그된 명언", 9);
        requested = quote(friendB, "요청한 명언", 10);
        rejected = quote(friendA, "거절된 명언", 11);
        mine = quote(viewer, "내 명언", 12);
        quote(stranger, "모르는 사람 명언", 13);
        quote(friendA, "다른 날 명언", DATE.minusDays(1).atTime(12, 0));

        em.persist(new QuoteTag(tagged, viewer));
        em.persist(new QuoteTag(tagged, friendB));
        em.persist(new QuoteTag(mine, friendA));
        em.persist(new QuoteLike(tagged, viewer));
        em.persist(new QuoteLike(requested, friendA));
        em.persist(Bookmark.builder().member(viewer).quote(tagged).build());
        em.persist(Bookmark.builder().member(viewer).quote(mine).build());
        em.persist(Bookmark.builder().member(friendA).quote(requested).build());
        tagRequest(requested, viewer, TagRequestStatus.PENDING);
        tagRequest(rejected, viewer, TagRequestStatus.REJECTED);
        tagRequest(tagged, friendB, TagRequestStatus.PENDING);
        em.flush();
        em.clear();
    }

    private Member member(String name) {
        Member member = Member.builder().email(name + "@test.com").nickname(name).password("password123").build();
        em.persist(member);
        return member;
    }

    private void friends(Member a, Member b) {
        em.persist(Friendship.builder().member(a).friend(b).status(FriendshipStatus.ACCEPTED).build());
        em.persist(Friendship.builder().member(b).friend(a).status(FriendshipStatus.ACCEPTED).build());
    }

    private Quote quote(Member author, String content, int hour) {
        return quote(author, content, DATE.atTime(hour, 0));
    }

    private Quote quote(Member author, String content, LocalDateTime createdAt) {
        Quote quote = Quote.builder().author(author).content(content).originalContent(content).build();
        em.persist(quote);
        em.flush();
        em.createNativeQuery("update quote set create_date = ?1 where id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, quote.getId())
                .executeUpdate();
        return quote;
    }

    private void tagRequest(Quote quote, Member requester, TagRequestStatus status) {
        em.persist(QuoteTagRequest.builder().quote(quote).requester(requester).status(status).build());
    }

    private ResultActions getAsViewer(String url) throws Exception {
        return mvc.perform(get(url).param("date", DATE.toString())
                .header("Authorization", "Bearer " + authTokenService.genToken(viewer, 600)));
    }

    private void assertOtherQuotes(ResultActions result, String path) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath(path + "[*].id", contains(rejected.getId().intValue(), requested.getId().intValue(), tagged.getId().intValue())))
                .andExpect(jsonPath(path + "[*].content", contains("거절된 명언", "요청한 명언", "태그된 명언")))
                .andExpect(jsonPath(path + "[*].authorNickname", contains("friendA", "friendB", "friendA")))
                .andExpect(jsonPath(path + "[*].isLiked", contains(false, false, true)))
                .andExpect(jsonPath(path + "[*].isBookmarked", contains(false, false, true)))
                .andExpect(jsonPath(path + "[*].isFriendQuote", contains(true, true, true)))
                .andExpect(jsonPath(path + "[*].myTagStatus", contains("NONE", "REQUESTED", "TAGGED")))
                .andExpect(jsonPath(path + "[0].taggedNicknames", empty()))
                .andExpect(jsonPath(path + "[1].tags", empty()))
                .andExpect(jsonPath(path + "[2].taggedNicknames", contains("viewer", "friendB")))
                .andExpect(jsonPath(path + "[2].tags[*].memberId", contains(viewer.getId().intValue(), friendB.getId().intValue())))
                .andExpect(jsonPath(path + "[2].tags[*].nickname", contains("viewer", "friendB")));
    }

    @Test
    @DisplayName("GET /api/quotes 응답 고정")
    void quoteList_response() throws Exception {
        ResultActions result = getAsViewer("/api/quotes");

        assertOtherQuotes(result, "$.otherQuotes");
        result.andExpect(jsonPath("$.myQuotes[*].id", contains(mine.getId().intValue())))
                .andExpect(jsonPath("$.myQuotes[0].groupName").value("아직 그룹이 없습니다."))
                .andExpect(jsonPath("$.myQuotes[0].isBookmarked").value(true))
                .andExpect(jsonPath("$.myQuotes[0].taggedNicknames", contains("friendA")))
                .andExpect(jsonPath("$.unwrittenMembers[*].nickname", contains("unwritten")));
    }

    @Test
    @DisplayName("GET /api/quotes/feed 응답 고정")
    void feed_response() throws Exception {
        assertOtherQuotes(getAsViewer("/api/quotes/feed"), "$.data.items");
    }

    private long queriesOf(Runnable action) {
        Statistics statistics = emf.unwrap(SessionFactory.class).getStatistics();
        em.flush();
        em.clear();
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    private void addMoreFriendQuotes() {
        for (int i = 0; i < 6; i++) {
            Quote quote = quote(friendB, "추가 명언 " + i, 14 + i);
            em.persist(new QuoteTag(quote, friendA));
            em.persist(new QuoteLike(quote, viewer));
            tagRequest(quote, viewer, TagRequestStatus.PENDING);
        }
    }

    @Test
    @DisplayName("명언 수가 늘어도 피드 쿼리 수는 같다")
    void feed_queryCountIsConstant() {
        Long viewerId = viewer.getId();
        long before = queriesOf(() -> quoteService.getFeed(viewerId, DATE, null));

        addMoreFriendQuotes();
        long after = queriesOf(() -> quoteService.getFeed(viewerId, DATE, null));

        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("명언 수가 늘어도 명언 목록 쿼리 수는 같다")
    void quoteList_queryCountIsConstant() {
        long before = queriesOf(() -> quoteService.getQuoteList(em.find(Member.class, viewer.getId()), DATE, null));

        addMoreFriendQuotes();
        long after = queriesOf(() -> quoteService.getQuoteList(em.find(Member.class, viewer.getId()), DATE, null));

        assertThat(after).isEqualTo(before);
    }
}
