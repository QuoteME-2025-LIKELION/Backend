package com.ll.demo.domain.quote.quote.controller;

import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupJoinRequest;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.entity.InviteType;
import com.ll.demo.domain.group.group.entity.JoinStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.Bookmark;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
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
class AdditiveResponseFieldsTest {

    @Autowired private MockMvc mvc;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private EntityManager em;

    private Member me;
    private Member leader;
    private Quote bookmarked;
    private Quote plain;

    @BeforeEach
    void setUp() {
        me = member("me");
        leader = member("leader");
        bookmarked = quote(me, "북마크한 명언");
        plain = quote(me, "북마크 안 한 명언");
        persist(Bookmark.builder().member(me).quote(bookmarked).build());
    }

    private Member member(String name) {
        return persist(Member.builder().email(name + "@test.com").nickname(name).password("password123").build());
    }

    private Quote quote(Member author, String content) {
        return persist(Quote.builder().author(author).content(content).originalContent(content).build());
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

    private String flag(Quote quote) {
        return "$[?(@.id == " + quote.getId() + ")].isBookmarked";
    }

    @Test
    @DisplayName("아카이브 내 명언: 명언별 isBookmarked")
    void archiveMine() throws Exception {
        getAsMe("/api/archives/me").andExpect(status().isOk())
                .andExpect(jsonPath(flag(bookmarked)).value(true))
                .andExpect(jsonPath(flag(plain)).value(false));
    }

    @Test
    @DisplayName("아카이브 날짜별: 명언별 isBookmarked")
    void archiveByDate() throws Exception {
        getAsMe("/api/archives?date=" + LocalDate.now()).andExpect(status().isOk())
                .andExpect(jsonPath(flag(bookmarked)).value(true))
                .andExpect(jsonPath(flag(plain)).value(false));
    }

    @Test
    @DisplayName("좋아요·북마크 목록: 명언별 isBookmarked")
    void archiveLikesAndBookmarks() throws Exception {
        persist(new QuoteLike(plain, me));
        getAsMe("/api/archives/likes").andExpect(status().isOk())
                .andExpect(jsonPath(flag(plain)).value(false));
        getAsMe("/api/archives/bookmarks").andExpect(status().isOk())
                .andExpect(jsonPath(flag(bookmarked)).value(true));
    }

    @Test
    @DisplayName("받은 초대: leaderNickname, memberCount 추가, 기존 필드 유지")
    void invitation() throws Exception {
        Member inviter = member("inviter");
        Group group = persist(Group.builder().name("그룹").motto("좌우명").leader(leader).build());
        persist(GroupMember.builder().group(group).member(leader).build());
        persist(GroupMember.builder().group(group).member(inviter).build());
        persist(GroupJoinRequest.builder().group(group).requester(me).inviter(inviter)
                .type(InviteType.INVITE).status(JoinStatus.PENDING).build());

        getAsMe("/api/groups/invitations").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inviterNickname").value("inviter"))
                .andExpect(jsonPath("$[0].leaderNickname").value("leader"))
                .andExpect(jsonPath("$[0].memberCount").value(2));
    }
}
