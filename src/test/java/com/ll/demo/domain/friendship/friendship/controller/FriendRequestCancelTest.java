package com.ll.demo.domain.friendship.friendship.controller;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.service.FriendshipService;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.repository.NotificationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FriendRequestCancelTest {

    @Autowired private MockMvc mvc;
    @Autowired private AuthTokenService authTokenService;
    @Autowired private FriendshipService friendshipService;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private EntityManager em;

    private Member me;
    private Member target;

    @BeforeEach
    void setUp() {
        me = memberRepository.save(Member.builder().email("me@test.com").nickname("나").password("password123").build());
        target = memberRepository.save(Member.builder().email("target@test.com").nickname("상대").password("password123").build());
    }

    private ResultActions cancel(Member actor, Member receiver) throws Exception {
        return mvc.perform(delete("/api/friends/request/" + receiver.getId())
                .header("Authorization", "Bearer " + authTokenService.genToken(actor, 600)));
    }

    private void flushClear() {
        em.flush();
        em.clear();
    }

    private long friendRequestNotifications() {
        return notificationRepository.findByReceiverIdAndTypeOrderByCreateDateDesc(target.getId(), "FRIEND_REQUEST").size();
    }

    @Test
    @DisplayName("보낸 대기 요청 취소 → 요청 행과 상대 알림 삭제")
    void cancel_deletesRequestAndNotification() throws Exception {
        friendshipService.sendFriendRequest(me, target.getId());
        flushClear();
        assertThat(friendRequestNotifications()).isEqualTo(1);

        cancel(me, target).andExpect(status().isOk());
        flushClear();

        assertThat(friendshipRepository.findByMemberAndFriend(me, target)).isEmpty();
        assertThat(friendRequestNotifications()).isZero();
    }

    @Test
    @DisplayName("상대가 나에게 보낸 요청은 취소할 수 없다 → 403")
    void cancelOthersRequest_403() throws Exception {
        friendshipService.sendFriendRequest(target, me.getId());
        flushClear();

        cancel(me, target).andExpect(status().isForbidden());
        flushClear();

        assertThat(friendshipRepository.findByMemberAndFriend(target, me)).isPresent();
    }

    @Test
    @DisplayName("요청이 없으면 404")
    void cancelMissing_404() throws Exception {
        cancel(me, target).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이미 수락된 요청은 취소할 수 없다 → 400")
    void cancelAccepted_400() throws Exception {
        friendshipRepository.save(Friendship.builder().member(me).friend(target).status(FriendshipStatus.ACCEPTED).build());
        flushClear();

        cancel(me, target).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("취소 후 같은 상대에게 다시 요청할 수 있다")
    void reRequestAfterCancel() throws Exception {
        friendshipService.sendFriendRequest(me, target.getId());
        flushClear();
        cancel(me, target).andExpect(status().isOk());
        flushClear();

        friendshipService.sendFriendRequest(me, target.getId());
        flushClear();

        assertThat(friendshipRepository.findByMemberAndFriend(me, target))
                .get().extracting(Friendship::getStatus).isEqualTo(FriendshipStatus.REQUESTED);
        assertThat(friendRequestNotifications()).isEqualTo(1);
    }

    @Test
    @DisplayName("보낸 목록에는 대기 중인 요청만 SENT로 표시된다")
    void sentList_pendingOnly() throws Exception {
        Member accepted = memberRepository.save(Member.builder().email("acc@test.com").nickname("수락").password("password123").build());
        friendshipService.sendFriendRequest(me, target.getId());
        friendshipRepository.save(Friendship.builder().member(me).friend(accepted).status(FriendshipStatus.ACCEPTED).build());
        flushClear();

        mvc.perform(get("/api/friends/requests")
                        .header("Authorization", "Bearer " + authTokenService.genToken(me, 600)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].direction").value("SENT"))
                .andExpect(jsonPath("$[0].receiverId").value(target.getId()));
    }
}
