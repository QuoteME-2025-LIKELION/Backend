package com.ll.demo.domain.friendship.friendship.service;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FriendReRequestTest {

    @Autowired private FriendshipService friendshipService;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private NotificationRepository notificationRepository;

    private Member a;
    private Member b;

    @BeforeEach
    void setUp() {
        a = memberRepository.save(Member.builder().email("a@test.com").nickname("에이").password("password123").build());
        b = memberRepository.save(Member.builder().email("b@test.com").nickname("비").password("password123").build());
    }

    private Friendship rejectedRequestFromAToB() {
        friendshipService.sendFriendRequest(a, b.getId());
        Friendship request = friendshipRepository.findByMemberAndFriend(a, b).orElseThrow();
        friendshipService.rejectFriendRequest(b, request.getId());
        return request;
    }

    private long friendRequestNotifications(Member receiver) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getType().equals("FRIEND_REQUEST") && n.getReceiver().getId().equals(receiver.getId()))
                .count();
    }

    private void assertConflict(Member requester, Member target) {
        assertThatThrownBy(() -> friendshipService.sendFriendRequest(requester, target.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    @DisplayName("거절 → 재요청 → 수락: 같은 레코드를 재사용하고 알림을 다시 보낸다")
    void rejectThenReRequestThenAccept() {
        Friendship request = rejectedRequestFromAToB();

        friendshipService.sendFriendRequest(a, b.getId());

        assertThat(request.getStatus()).isEqualTo(FriendshipStatus.REQUESTED);
        assertThat(friendRequestNotifications(b)).isEqualTo(2);

        friendshipService.acceptFriendRequest(b, request.getId());

        assertThat(friendshipRepository.existsByMemberAndFriendAndStatus(a, b, FriendshipStatus.ACCEPTED)).isTrue();
        assertThat(friendshipRepository.existsByMemberAndFriendAndStatus(b, a, FriendshipStatus.ACCEPTED)).isTrue();
    }

    @Test
    @DisplayName("반대 방향 거절 레코드는 삭제하고 새 요청을 만든다")
    void reverseRejected_replaced() {
        rejectedRequestFromAToB();

        friendshipService.sendFriendRequest(b, a.getId());

        assertThat(friendshipRepository.findByMemberAndFriend(a, b)).isEmpty();
        assertThat(friendshipRepository.findByMemberAndFriend(b, a).orElseThrow().getStatus())
                .isEqualTo(FriendshipStatus.REQUESTED);
        assertThat(friendRequestNotifications(a)).isEqualTo(1);
    }

    @Test
    @DisplayName("대기 중이거나 이미 친구면 양방향 모두 409 유지")
    void pendingOrAccepted_conflict() {
        friendshipService.sendFriendRequest(a, b.getId());
        assertConflict(a, b);
        assertConflict(b, a);

        friendshipService.acceptFriendRequest(b, friendshipRepository.findByMemberAndFriend(a, b).orElseThrow().getId());
        assertConflict(a, b);
        assertConflict(b, a);
    }
}
