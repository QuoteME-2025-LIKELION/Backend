package com.ll.demo.domain.friendship.friendship.service;

import com.ll.demo.domain.friendship.friendship.dto.FriendRequestResponse;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendRequestDirection;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.dto.FriendResponse;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
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
class FriendshipServiceTest {

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member a;
    private Member b;

    @BeforeEach
    void setUp() {
        a = memberRepository.save(Member.builder()
                .email("a@test.com").nickname("에이").password("password123").build());
        b = memberRepository.save(Member.builder()
                .email("b@test.com").nickname("비").password("password123").build());
    }

    @Test
    @DisplayName("요청 전: 양쪽 모두 친구도 요청도 없다")
    void beforeRequest() {
        assertThat(memberService.getFriendList(a.getId())).isEmpty();
        assertThat(memberService.getFriendList(b.getId())).isEmpty();
        assertThat(friendshipService.getFriendRequests(a)).isEmpty();
        assertThat(friendshipService.getFriendRequests(b)).isEmpty();
    }

    @Test
    @DisplayName("요청 중: 보낸 쪽 친구 목록에 없고, 보낸/받은 요청 목록에 각각 표시된다")
    void whileRequested() {
        friendshipService.sendFriendRequest(a, b.getId());

        assertThat(memberService.getFriendList(a.getId())).isEmpty();
        assertThat(memberService.getFriendList(b.getId())).isEmpty();
        assertThat(friendshipRepository.findFriendIds(a.getId())).isEmpty();
        assertThat(friendshipRepository.countByMemberAndStatus(a, FriendshipStatus.ACCEPTED)).isZero();

        List<FriendRequestResponse> aRequests = friendshipService.getFriendRequests(a);
        assertThat(aRequests).hasSize(1);
        assertThat(aRequests.get(0).getDirection()).isEqualTo(FriendRequestDirection.SENT);
        assertThat(aRequests.get(0).getReceiverId()).isEqualTo(b.getId());

        List<FriendRequestResponse> bRequests = friendshipService.getFriendRequests(b);
        assertThat(bRequests).hasSize(1);
        assertThat(bRequests.get(0).getDirection()).isEqualTo(FriendRequestDirection.RECEIVED);
        assertThat(bRequests.get(0).getSenderId()).isEqualTo(a.getId());
    }

    @Test
    @DisplayName("수락 후: 양쪽 친구 목록에 서로 포함되고 요청 목록은 비어 있다")
    void afterAccept() {
        friendshipService.sendFriendRequest(a, b.getId());
        Long requestId = friendshipService.getFriendRequests(b).get(0).getRequestId();

        friendshipService.acceptFriendRequest(b, requestId);

        assertThat(memberService.getFriendList(a.getId()))
                .extracting(FriendResponse::getId).containsExactly(b.getId());
        assertThat(memberService.getFriendList(b.getId()))
                .extracting(FriendResponse::getId).containsExactly(a.getId());
        assertThat(friendshipRepository.findFriendIds(a.getId())).containsExactly(b.getId());
        assertThat(friendshipService.getFriendRequests(a)).isEmpty();
        assertThat(friendshipService.getFriendRequests(b)).isEmpty();
    }

    @Test
    @DisplayName("거절 후: 양쪽 친구 목록과 요청 목록 모두 비어 있다")
    void afterReject() {
        friendshipService.sendFriendRequest(a, b.getId());
        Long requestId = friendshipService.getFriendRequests(b).get(0).getRequestId();

        friendshipService.rejectFriendRequest(b, requestId);

        assertThat(memberService.getFriendList(a.getId())).isEmpty();
        assertThat(memberService.getFriendList(b.getId())).isEmpty();
        assertThat(friendshipService.getFriendRequests(a)).isEmpty();
        assertThat(friendshipService.getFriendRequests(b)).isEmpty();
    }
}