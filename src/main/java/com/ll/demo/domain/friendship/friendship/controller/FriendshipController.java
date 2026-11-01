package com.ll.demo.domain.friendship.friendship.controller;

import com.ll.demo.domain.friendship.friendship.dto.FriendRequestResponse;
import com.ll.demo.domain.friendship.friendship.service.FriendshipService;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    // 친구 요청 전송
    @PostMapping("/request/{targetId}")
    public ResponseEntity<RsData> sendFriendRequest(
            @PathVariable Long targetId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        friendshipService.sendFriendRequest(user.getMember(), targetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(RsData.of("201-1", "친구 요청을 보냈습니다."));
    }

    // 친구 요청 목록 조회 (받은 요청 + 보낸 요청)
    @GetMapping("/requests")
    public ResponseEntity<List<FriendRequestResponse>> getFriendRequests(
            @AuthenticationPrincipal SecurityUser user
    ) {
        return ResponseEntity.ok(friendshipService.getFriendRequests(user.getMember()));
    }

    // 친구 요청 수락
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<RsData> acceptFriendRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        friendshipService.acceptFriendRequest(user.getMember(), requestId);
        return ResponseEntity.ok(RsData.of("200", "친구 요청을 수락했습니다."));
    }

    // 친구 요청 거절
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<RsData> rejectFriendRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal SecurityUser user
    ) {
        friendshipService.rejectFriendRequest(user.getMember(), requestId);
        return ResponseEntity.ok(RsData.of("200", "친구 요청을 거절했습니다."));
    }

    // 친구 삭제
    @DeleteMapping("/{friendId}")
    public ResponseEntity<RsData> removeFriend(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long friendId
    ) {
        friendshipService.removeFriend(user.getMember(), friendId);
        return ResponseEntity.ok(RsData.of("200", "친구 삭제 완료"));
    }
}
