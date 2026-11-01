package com.ll.demo.domain.quote.quote.controller;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteTag;
import com.ll.demo.domain.quote.repository.QuoteRepository;
import com.ll.demo.domain.quote.repository.QuoteTagRepository;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.persistence.EntityManager;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuoteTagDetailResponseTest {

    @Autowired private MockMvc mvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private QuoteTagRepository quoteTagRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private EntityManager em;

    private Member author;
    private Member friend;
    private Member tagged;

    @BeforeEach
    void setUp() {
        author = save("author", null);
        friend = save("friend", null);
        tagged = save("tagged", "https://img.test/tagged.png");
        friendshipRepository.save(Friendship.builder().member(friend).friend(author).status(FriendshipStatus.ACCEPTED).build());
        Quote quote = quoteRepository.save(Quote.builder().author(author).content("명언").originalContent("원문").build());
        quoteTagRepository.save(new QuoteTag(quote, tagged));
        em.flush();
        em.clear();
    }

    private Member save(String name, String profileImage) {
        return memberRepository.save(Member.builder().email(name + "@test.com").nickname(name).password("password123")
                .profileImage(profileImage).build());
    }

    private ResultActions getAs(Member member, String url) throws Exception {
        return mvc.perform(get(url).param("date", LocalDate.now().toString())
                .header("Authorization", "Bearer " + authTokenService.genToken(member, 600)));
    }

    private void assertTag(ResultActions result, String prefix) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(jsonPath(prefix + ".tags[0].memberId").value(tagged.getId()))
                .andExpect(jsonPath(prefix + ".tags[0].nickname").value("tagged"))
                .andExpect(jsonPath(prefix + ".tags[0].profileImage").value("https://img.test/tagged.png"));
    }

    @Test
    @DisplayName("피드 응답: 기존 닉네임 유지, tags 추가")
    void feed() throws Exception {
        assertTag(getAs(friend, "/api/quotes/feed")
                .andExpect(jsonPath("$.data.items[0].taggedNicknames[0]").value("tagged")), "$.data.items[0]");
    }

    @Test
    @DisplayName("명언 목록 응답: 내 명언, 친구 명언 모두 tags 포함")
    void quoteList() throws Exception {
        assertTag(getAs(author, "/api/quotes")
                .andExpect(jsonPath("$.myQuotes[0].taggedNicknames[0]").value("tagged")), "$.myQuotes[0]");
        assertTag(getAs(friend, "/api/quotes"), "$.otherQuotes[0]");
    }

    @Test
    @DisplayName("아카이브 응답: tags 포함")
    void archive() throws Exception {
        assertTag(getAs(author, "/api/archives/me"), "$[0]");
    }
}
