package com.ll.demo.domain.quote.quote.controller;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.Quote;
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

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiGeneratedFlagTest {

    @Autowired private MockMvc mvc;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private EntityManager em;

    private Member me;
    private Quote nullSummary;
    private Quote emptySummary;
    private Quote blankSummary;
    private Quote aiSummary;

    @BeforeEach
    void setUp() {
        me = persist(Member.builder().email("ai-me@test.com").nickname("나").password("password123").build());
        Member friend = persist(Member.builder().email("ai-friend@test.com").nickname("친구").password("password123").build());
        persist(Friendship.builder().member(me).friend(friend).status(FriendshipStatus.ACCEPTED).build());
        persist(Friendship.builder().member(friend).friend(me).status(FriendshipStatus.ACCEPTED).build());
        nullSummary = quote(friend, null);
        emptySummary = quote(friend, "");
        blankSummary = quote(friend, "  ");
        aiSummary = quote(friend, "AI가 만든 명언");
    }

    private Quote quote(Member author, String summary) {
        return persist(Quote.builder().author(author).content("직접 쓴 명언").originalContent("일기").summary(summary).build());
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    private ResultActions getAsMe(String url) throws Exception {
        em.flush();
        em.clear();
        return mvc.perform(get(url).header("Authorization", "Bearer " + authTokenService.genToken(me, 600)));
    }

    private String flag(String prefix, Quote quote) {
        return prefix + "[?(@.id == " + quote.getId() + ")].isAiGenerated";
    }

    @Test
    @DisplayName("피드 items: summary가 null·빈 문자열·공백이면 false, 값이 있으면 true")
    void feed() throws Exception {
        getAsMe("/api/quotes/feed?date=" + LocalDate.now())
                .andExpect(status().isOk())
                .andExpect(jsonPath(flag("$.data.items", nullSummary)).value(contains(false)))
                .andExpect(jsonPath(flag("$.data.items", emptySummary)).value(contains(false)))
                .andExpect(jsonPath(flag("$.data.items", blankSummary)).value(contains(false)))
                .andExpect(jsonPath(flag("$.data.items", aiSummary)).value(contains(true)));
    }

    @Test
    @DisplayName("명언 목록 otherQuotes에도 같은 기준의 isAiGenerated 키가 나온다")
    void quoteList() throws Exception {
        getAsMe("/api/quotes?date=" + LocalDate.now())
                .andExpect(status().isOk())
                .andExpect(jsonPath(flag("$.otherQuotes", nullSummary)).value(contains(false)))
                .andExpect(jsonPath(flag("$.otherQuotes", emptySummary)).value(contains(false)))
                .andExpect(jsonPath(flag("$.otherQuotes", blankSummary)).value(contains(false)))
                .andExpect(jsonPath(flag("$.otherQuotes", aiSummary)).value(contains(true)));
    }
}
