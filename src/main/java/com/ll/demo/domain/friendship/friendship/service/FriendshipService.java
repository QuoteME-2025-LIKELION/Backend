package com.ll.demo.domain.friendship.friendship.service;

import com.ll.demo.domain.friendship.friendship.dto.FriendRequestResponse;
import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.repository.FriendshipRepository;
import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.friendship.friendship.type.FriendRequestDirection;
import com.ll.demo.global.exceptions.GlobalException;

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
    private final NotificationRepository notificationRepository;

    // 친구 요청 전송 (단방향 REQUESTED)
    @Transactional
    public void sendFriendRequest(Member requester, Long targetId) {
        Member target = memberRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 회원을 찾을 수 없습니다."));

        if (requester.getId().equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신에게 요청할 수 없습니다.");
        }

        Friendship sent = friendshipRepository.findByMemberAndFriend(requester, target).orElse(null);
        Friendship received = friendshipRepository.findByMemberAndFriend(target, requester).orElse(null);

        // 양방향 모두 거절 외 레코드가 있으면 차단 (이미 친구이거나 대기 중)
        if (isActive(sent) || isActive(received)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 친구이거나 대기 중인 요청이 있습니다.");
        }

        // 거절된 뒤에는 다시 요청할 수 있음
        if (received != null) {
            friendshipRepository.delete(received);
        }
        if (sent != null) {
            sent.updateStatus(FriendshipStatus.REQUESTED);
        } else {
            friendshipRepository.save(Friendship.builder()
                    .member(requester)
                    .friend(target)
                    .status(FriendshipStatus.REQUESTED)
                    .build());
        }

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

    // 보낸 친구 요청 취소 (대기 중인 내 요청만, 상대에게 간 요청 알림도 삭제)
    @Transactional
    public void cancelFriendRequest(Member me, Long receiverId) {
        Member receiver = memberRepository.findById(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "대상 회원을 찾을 수 없습니다."));

        Friendship request = friendshipRepository.findByMemberAndFriend(me, receiver).orElse(null);
        if (request == null) {
            boolean receivedPending = friendshipRepository.existsByMemberAndFriendAndStatus(receiver, me, FriendshipStatus.REQUESTED);
            if (receivedPending) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내가 보낸 요청만 취소할 수 있습니다.");
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "요청을 찾을 수 없습니다.");
        }
        if (request.getStatus() != FriendshipStatus.REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "처리할 수 없는 요청입니다.");
        }

        friendshipRepository.delete(request);
        notificationRepository.deleteSentNotification("FRIEND_REQUEST", me, receiver, me.getId());
    }

    private boolean isActive(Friendship friendship) {
        return friendship != null && friendship.getStatus() != FriendshipStatus.REJECTED;
    }

    // 친구 삭제
    @Transactional
    public void removeFriend(Member actor, Long friendId) {
        Member friend = memberRepository.findById(friendId)
                .orElseThrow(() -> new GlobalException("404", "해당 회원을 찾을 수 없습니다."));
        friendshipRepository.deleteByMemberAndFriend(actor, friend);
        friendshipRepository.deleteByMemberAndFriend(friend, actor);
    }
}
