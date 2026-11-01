package com.ll.demo.domain.archive;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.Bookmark;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookmarkMonthFilterTest {

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private AuthTokenService authTokenService;

    private Member me;

    @BeforeEach
    void setUp() {
        me = member("me");
        Member other = member("other");
        Member author = member("author");

        bookmark(me, quote(author, "3월 말", LocalDateTime.of(2026, 3, 31, 23, 59, 59)));
        bookmark(me, quote(author, "4월 처음", LocalDateTime.of(2026, 4, 1, 0, 0, 0)));
        bookmark(me, quote(author, "4월 끝", LocalDateTime.of(2026, 4, 30, 23, 59, 59)));
        bookmark(me, quote(author, "5월 처음", LocalDateTime.of(2026, 5, 1, 0, 0, 0)));
        bookmark(other, quote(author, "남의 북마크", LocalDateTime.of(2026, 4, 15, 12, 0, 0)));
        em.flush();
        em.clear();
    }

    private Member member(String name) {
        Member member = Member.builder().email(name + "@test.com").nickname(name).password("password123").build();
        em.persist(member);
        return member;
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

    private void bookmark(Member member, Quote quote) {
        em.persist(Bookmark.builder().member(member).quote(quote).build());
    }

    private ResultActions getBookmarks(String query) throws Exception {
        return mvc.perform(get("/api/archives/bookmarks" + query)
                .header("Authorization", "Bearer " + authTokenService.genToken(me, 600)));
    }

    @Test
    @DisplayName("명언 작성일 기준 반열린 구간, 다른 사용자 북마크 미포함")
    void monthBoundaries() throws Exception {
        getBookmarks("?month=2026-04").andExpect(status().isOk())
                .andExpect(jsonPath("$[*].content", containsInAnyOrder("4월 처음", "4월 끝")));
    }

    @Test
    @DisplayName("month 미지정이면 기존처럼 전체")
    void noMonth_returnsAll() throws Exception {
        getBookmarks("").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @DisplayName("month 형식 오류 → 400")
    void badMonth_400() throws Exception {
        getBookmarks("?month=2026-13").andExpect(status().isBadRequest());
    }
}
