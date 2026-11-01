package com.ll.demo.domain.notification.notification.service;

import com.ll.demo.domain.group.group.entity.Group;
import com.ll.demo.domain.group.group.entity.GroupMember;
import com.ll.demo.domain.group.group.service.GroupService;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.notification.dto.NotificationResponse;
import com.ll.demo.domain.notification.entity.Notification;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.profile.profile.service.ProfileService;
import com.ll.demo.domain.quote.entity.Quote;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrphanNotificationTest {

    @Autowired private ProfileService profileService;
    @Autowired private GroupService groupService;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private EntityManager em;

    private Member leaver;
    private Member receiver;
    private Member sender;

    @BeforeEach
    void setUp() {
        leaver = member("leaver");
        receiver = member("receiver");
        sender = member("sender");
    }

    private Member member(String name) {
        Member member = Member.builder().email(name + "@test.com").nickname(name).password("password123").build();
        em.persist(member);
        return member;
    }

    private Quote quote(Member author) {
        Quote quote = Quote.builder().author(author).content("명언").originalContent("원문").build();
        em.persist(quote);
        return quote;
    }

    private Group group(Member leader) {
        Group group = Group.builder().name("그룹").motto("좌우명").leader(leader).build();
        em.persist(group);
        em.persist(GroupMember.builder().group(group).member(leader).build());
        return group;
    }

    private Notification notification(String type, Long targetId) {
        Notification notification = Notification.builder()
                .receiver(receiver).sender(sender).type(type).message("알림").targetId(targetId).build();
        em.persist(notification);
        return notification;
    }

    private List<String> remainingTypes() {
        em.flush();
        em.clear();
        return notificationRepository.findAll().stream().map(Notification::getType).toList();
    }

    @Test
    @DisplayName("탈퇴 후 탈퇴자 명언을 가리키는 다른 회원의 알림은 0건, 무관한 알림은 유지")
    void deleteAccount_removesQuoteNotifications() {
        Quote leaverQuote = quote(leaver);
        Quote otherQuote = quote(sender);
        em.flush();
        notification("LIKE", leaverQuote.getId());
        notification("TAG", leaverQuote.getId());
        notification("TAG_REQUEST", leaverQuote.getId());
        notification("TAG_ACCEPTED", leaverQuote.getId());
        notification("TAG", otherQuote.getId());
        notification("FRIEND_REQUEST", leaverQuote.getId());
        em.flush();
        em.clear();

        profileService.deleteAccount(em.find(Member.class, leaver.getId()));

        assertThat(remainingTypes()).containsExactlyInAnyOrder("TAG", "FRIEND_REQUEST");
    }

    @Test
    @DisplayName("그룹 삭제 후 초대·가입 요청 알림 0건, 다른 그룹과 다른 타입은 유지")
    void deleteGroup_removesGroupNotifications() {
        Group deleted = group(leaver);
        Group other = group(sender);
        em.flush();
        notification("GROUP_INVITE", deleted.getId());
        notification("GROUP_JOIN_REQUEST", deleted.getId());
        notification("GROUP_INVITE", other.getId());
        notification("LIKE", deleted.getId());
        em.flush();
        em.clear();

        groupService.deleteGroup(em.find(Member.class, leaver.getId()), deleted.getId());

        assertThat(remainingTypes()).containsExactlyInAnyOrder("GROUP_INVITE", "LIKE");
    }

    @Test
    @DisplayName("탈퇴로 혼자 남은 그룹이 삭제되면 초대 알림도 0건")
    void deleteAccount_soloGroup_removesGroupNotifications() {
        Group solo = group(leaver);
        em.flush();
        notification("GROUP_INVITE", solo.getId());
        notification("GROUP_JOIN_REQUEST", solo.getId());
        em.flush();
        em.clear();

        profileService.deleteAccount(em.find(Member.class, leaver.getId()));

        assertThat(remainingTypes()).isEmpty();
    }

    @Test
    @DisplayName("대상 명언이 없는 LIKE 알림은 목록에서 제외, 있는 것은 유지")
    void missingLikeTarget_excludedFromList() {
        Quote quote = quote(receiver);
        em.flush();
        notification("LIKE", quote.getId());
        notification("LIKE", 999_999L);
        em.flush();
        em.clear();

        List<NotificationResponse> result = notificationService.findMyNotifications(receiver.getId(), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetId()).isEqualTo(quote.getId());
    }
}
