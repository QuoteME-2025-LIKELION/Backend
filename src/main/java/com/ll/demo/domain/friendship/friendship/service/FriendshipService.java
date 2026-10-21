package com.ll.demo.domain.friendship.friendship.service;

import com.ll.demo.domain.friendship.friendship.dto.FriendRequestResponse;
import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.friendship.friendship.type.FriendRequestDirection;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendshipService {
    private final FriendshipRepository friendshipRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    // 친구 요청 전송 (단방향 REQUESTED)
    @Transactional
    public void sendFriendRequest(Member requester, Long targetId) {
        Member target = memberRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 회원을 찾을 수 없습니다."));

        if (requester.getId().equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신에게 요청할 수 없습니다.");
        }

        // 양방향 모두 이미 레코드가 있으면 차단 (이미 친구이거나 대기 중)
        if (friendshipRepository.existsByMemberAndFriend(requester, target) ||
                friendshipRepository.existsByMemberAndFriend(target, requester)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 친구이거나 대기 중인 요청이 있습니다.");
        }

        friendshipRepository.save(Friendship.builder()
                .member(requester)
                .friend(target)
                .status(FriendshipStatus.REQUESTED)
                .build());

        notificationService.create(
                target,
                requester,
                "FRIEND_REQUEST",
                requester.getNickname() + "님이 친구 요청을 보냈습니다.",
                requester.getId()
        );
    }

    // 친구 요청 목록 조회 (받은 요청 + 보낸 요청)
    public List<FriendRequestResponse> getFriendRequests(Member member) {
        Stream<FriendRequestResponse> received = friendshipRepository
                .findByFriendAndStatus(member, FriendshipStatus.REQUESTED)
                .stream()
                .map(f -> FriendRequestResponse.from(f, FriendRequestDirection.RECEIVED));
        Stream<FriendRequestResponse> sent = friendshipRepository
                .findAllByMemberAndStatus(member, FriendshipStatus.REQUESTED)
                .stream()
                .map(f -> FriendRequestResponse.from(f, FriendRequestDirection.SENT));
        return Stream.concat(received, sent).toList();
    }

    // 친구 요청 수락
    @Transactional
    public void acceptFriendRequest(Member me, Long requestId) {
        Friendship request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."));

        if (!request.getFriend().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
        if (request.getStatus() != FriendshipStatus.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "처리할 수 없는 요청입니다.");
        }

        // 기존 레코드 ACCEPTED 처리
        request.updateStatus(FriendshipStatus.ACCEPTED);

        // 역방향 레코드 생성 (양방향 친구 관계 완성)
        friendshipRepository.save(Friendship.builder()
                .member(me)
                .friend(request.getMember())
                .status(FriendshipStatus.ACCEPTED)
                .build());

        notificationService.create(
                request.getMember(),
                me,
                "FRIEND_ACCEPTED",
                me.getNickname() + "님이 친구 요청을 수락했습니다.",
                me.getId()
        );
    }

    // 친구 요청 거절
    @Transactional
    public void rejectFriendRequest(Member me, Long requestId) {
        Friendship request = friendshipRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다."));

        if (!request.getFriend().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
        if (request.getStatus() != FriendshipStatus.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "처리할 수 없는 요청입니다.");
        }

        request.updateStatus(FriendshipStatus.REJECTED);

        notificationService.create(
                request.getMember(),
                me,
                "FRIEND_REJECTED",
                me.getNickname() + "님이 친구 요청을 거절했습니다.",
                me.getId()
        );
    }

    // 친구 삭제
    @Transactional
    public void removeFriend(Member actor, Long friendId) {
        Member friend = memberRepository.findById(friendId)
                .orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));
        friendshipRepository.deleteByMemberAndFriend(actor, friend);
        friendshipRepository.deleteByMemberAndFriend(friend, actor);
    }
}
