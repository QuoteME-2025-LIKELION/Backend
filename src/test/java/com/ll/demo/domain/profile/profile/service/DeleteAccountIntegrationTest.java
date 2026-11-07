package com.ll.demo.domain.profile.profile.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupJoinRequest;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.entity.InviteType;
import com.ll.demo.domain.group.group.entity.JoinStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.entity.MemberSocialAccount;
import com.ll.demo.domain.member.member.type.MemberProvider;
import com.ll.demo.domain.notification.entity.Notification;
import com.ll.demo.domain.notification.entity.NotificationSetting;
import com.ll.demo.domain.poke.entity.Poke;
import com.ll.demo.domain.push.entity.DevicePlatform;
import com.ll.demo.domain.push.entity.DeviceToken;
import com.ll.demo.domain.quote.entity.Bookmark;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.entity.QuoteLike;
import com.ll.demo.domain.quote.entity.QuoteTag;
import com.ll.demo.domain.quote.entity.QuoteTagRequest;
import com.ll.demo.domain.quote.entity.TagRequestStatus;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DeleteAccountIntegrationTest {

    // 회원을 참조하는 모든 FK. 엔티티가 늘면 함께 추가한다.
    private static final Map<String, String> MEMBER_REFERENCES = Map.ofEntries(
            Map.entry("Quote", "author"),
            Map.entry("QuoteLike", "member"),
            Map.entry("QuoteTag", "member"),
            Map.entry("QuoteTagRequest", "requester"),
            Map.entry("Bookmark", "member"),
            Map.entry("Friendship", "member"),
            Map.entry("Group", "leader"),
            Map.entry("GroupMember", "member"),
            Map.entry("GroupJoinRequest", "requester"),
            Map.entry("Poke", "sender"),
            Map.entry("Notification", "receiver"),
            Map.entry("NotificationSetting", "member"),
            Map.entry("MemberSocialAccount", "member"),
            Map.entry("DeviceToken", "member")
    );
    private static final Map<String, String> SECOND_MEMBER_REFERENCES = Map.of(
            "Friendship", "friend",
            "GroupJoinRequest", "inviter",
            "Poke", "receiver",
            "Notification", "sender"
    );

    @Autowired private ProfileService profileService;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private EntityManager em;
    @Autowired private MockMvc mvc;

    private Member member(String name) {
        return persist(Member.builder().email(name + "@test.com").nickname(name).password("password123").build());
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    private Quote quote(Member author, String content) {
        return persist(Quote.builder().author(author).content(content).originalContent(content).build());
    }

    private Group group(String name, Member leader) {
        return persist(Group.builder().name(name).motto("좌우명").leader(leader).build());
    }

    private void join(Group group, Member member) {
        persist(GroupMember.builder().group(group).member(member).build());
    }

    private void link(Quote quote, Member member) {
        persist(new QuoteLike(quote, member));
        persist(new QuoteTag(quote, member));
        persist(Bookmark.builder().quote(quote).member(member).build());
        persist(QuoteTagRequest.builder().quote(quote).requester(member).status(TagRequestStatus.PENDING).build());
    }

    private void friendship(Member from, Member to, FriendshipStatus status) {
        persist(Friendship.builder().member(from).friend(to).status(status).build());
    }

    private void poke(Member from, Member to) {
        persist(Poke.builder().sender(from).receiver(to).build());
        persist(Notification.builder().sender(from).receiver(to).type("POKE").message("콕").targetId(from.getId()).build());
    }

    private void joinRequest(Group group, Member requester, Member inviter, InviteType type) {
        persist(GroupJoinRequest.builder().group(group).requester(requester).inviter(inviter)
                .type(type).status(JoinStatus.PENDING).build());
    }

    private long count(String entity, String field, Long memberId) {
        return em.createQuery("select count(e) from " + entity + " e where e." + field + ".id = :id", Long.class)
                .setParameter("id", memberId)
                .getSingleResult();
    }

    private String bearer(Member member) {
        return "Bearer " + authTokenService.genToken(member, 600);
    }

    @Test
    @DisplayName("연관 데이터를 모두 가진 회원 탈퇴 → 참조 0건, 리더 위임, 혼자인 그룹 삭제, 남은 회원 조회 정상")
    void deleteAccount_removesEverything() throws Exception {
        Member x = member("x");
        Member earliest = member("earliest");
        Member later = member("later");
        Member requester = member("requester");

        Quote xQuote = quote(x, "탈퇴자의 명언");
        Quote otherQuote = quote(later, "남의 명언");
        link(xQuote, later);
        link(xQuote, earliest);
        link(otherQuote, x);

        friendship(x, earliest, FriendshipStatus.ACCEPTED);
        friendship(earliest, x, FriendshipStatus.ACCEPTED);
        friendship(x, later, FriendshipStatus.REQUESTED);
        friendship(requester, x, FriendshipStatus.REQUESTED);
        friendship(earliest, later, FriendshipStatus.ACCEPTED);
        friendship(later, earliest, FriendshipStatus.ACCEPTED);

        Group shared = group("위임될 그룹", x);
        join(shared, x);
        join(shared, earliest);
        join(shared, later);
        Group solo = group("혼자인 그룹", x);
        join(solo, x);
        joinRequest(solo, requester, null, InviteType.JOIN_REQUEST);
        Group ledByOther = group("남의 그룹", later);
        join(ledByOther, later);
        join(ledByOther, x);
        Group pending = group("초대받은 그룹", earliest);
        join(pending, earliest);
        joinRequest(pending, x, earliest, InviteType.INVITE);
        joinRequest(ledByOther, requester, x, InviteType.INVITE);
        joinRequest(ledByOther, x, null, InviteType.JOIN_REQUEST);

        poke(x, later);
        poke(later, x);
        persist(NotificationSetting.builder().member(x).build());
        persist(MemberSocialAccount.builder().member(x).provider(MemberProvider.GOOGLE).providerId("x-google").build());
        persist(MemberSocialAccount.builder().member(x).provider(MemberProvider.KAKAO).providerId("x-kakao").build());
        persist(DeviceToken.builder().member(x).token("x-web-token").platform(DevicePlatform.WEB).build());
        persist(DeviceToken.builder().member(x).token("x-android-token").platform(DevicePlatform.ANDROID).build());
        em.flush();
        em.clear();

        Long xId = x.getId();
        Long sharedId = shared.getId();
        Long soloId = solo.getId();
        assertThat(count("DeviceToken", "member", xId)).isEqualTo(2);

        profileService.deleteAccount(em.find(Member.class, xId));
        em.flush();
        em.clear();

        MEMBER_REFERENCES.forEach((entity, field) ->
                assertThat(count(entity, field, xId)).as(entity + "." + field).isZero());
        SECOND_MEMBER_REFERENCES.forEach((entity, field) ->
                assertThat(count(entity, field, xId)).as(entity + "." + field).isZero());
        assertThat(em.find(Member.class, xId)).isNull();
        assertThat(em.find(Quote.class, xQuote.getId())).isNull();
        assertThat(em.find(Quote.class, otherQuote.getId())).isNotNull();
        assertThat(em.find(Group.class, soloId)).isNull();
        assertThat(em.createQuery("select count(r) from GroupJoinRequest r where r.group.id = :id", Long.class)
                .setParameter("id", soloId).getSingleResult()).isZero();
        assertThat(em.createQuery("select count(g) from GroupMember g where g.group.id = :id", Long.class)
                .setParameter("id", soloId).getSingleResult()).isZero();
        assertThat(em.find(Group.class, sharedId).getLeader().getId()).isEqualTo(earliest.getId());

        String today = LocalDate.now().toString();
        mvc.perform(get("/api/quotes/feed").param("date", today).header("Authorization", bearer(later)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/quotes").param("date", today).header("Authorization", bearer(later)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/groups/" + sharedId).header("Authorization", bearer(later)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderNickname").value("earliest"))
                .andExpect(jsonPath("$.memberCount").value(2));
        mvc.perform(get("/api/settings/friends-list").header("Authorization", bearer(later)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("탈퇴 응답은 access·refresh 쿠키를 만료시킨다")
    void deleteAccount_expiresCookies() throws Exception {
        Member x = member("x");
        em.flush();

        mvc.perform(delete("/api/profile/account").header("Authorization", bearer(x)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("accessToken", 0))
                .andExpect(cookie().maxAge("refreshToken", 0));
    }
}
