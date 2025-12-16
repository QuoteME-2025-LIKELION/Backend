package com.ll.demo.domain.group.group.controller;

import com.ll.demo.domain.group.group.dto.GroupRequest;
import com.ll.demo.domain.group.group.dto.GroupResponse;
import com.ll.demo.domain.group.group.service.GroupService;
import com.ll.demo.global.security.SecurityUser;
import com.ll.demo.domain.group.group.dto.MottoRequest;
import com.ll.demo.domain.group.group.dto.GroupDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    // 그룹 생성
    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
                                                      @AuthenticationPrincipal SecurityUser user,
                                                      @Valid @RequestBody GroupRequest req
    ) {
        GroupResponse response = groupService.createGroup(user.getMember(), req);
        return ResponseEntity.ok(response);
    }

    // 내가 가입한 그룹 조회
    @GetMapping({"/me", "/me/"})
    public ResponseEntity<List<GroupResponse>> getMyGroups(@AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(groupService.getMyGroups(user.getMember()));
    }

    // 그룹 초대
    @PostMapping("/{groupId}/invite/{friendId}")
    public ResponseEntity<String> invite(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long groupId,
            @PathVariable Long friendId
    ) {
        groupService.inviteFriend(user.getMember(), groupId, friendId);
        return ResponseEntity.ok("초대가 완료되었습니다.");
    }

    // 그룹 친구 삭제
    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<String> removeOrLeave(@AuthenticationPrincipal SecurityUser user, @PathVariable Long groupId, @PathVariable Long memberId) {
        groupService.removeOrLeaveMember(user.getMember(), groupId, memberId);
        return ResponseEntity.ok("완료되었습니다.");
    }

    // 그룹 가입 요청
    @PostMapping("/{groupId}/join-request")
    public ResponseEntity<String> joinRequest(@AuthenticationPrincipal SecurityUser user, @PathVariable Long groupId) {
        groupService.requestToJoin(user.getMember(), groupId);
        return ResponseEntity.ok("참여 요청이 발송되었습니다.");
    }


    @PostMapping("/join-requests/{requestId}/accept")
    public ResponseEntity<String> acceptRequest(@AuthenticationPrincipal SecurityUser user, @PathVariable Long requestId) {
        groupService.acceptJoinRequest(user.getMember(), requestId);
        return ResponseEntity.ok("가입 승인이 완료되었습니다.");
    }

    // 특정 그룹 상세 조회
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailResponse> getGroupDetail(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupDetail(groupId));
    }

    // 그룹 삭제 API
    @DeleteMapping("/{groupId}")
    public ResponseEntity<String> deleteGroup(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long groupId
    ) {
        groupService.deleteGroup(user.getMember(), groupId);
        return ResponseEntity.ok("그룹이 삭제되었습니다.");
    }

    // 그룹 메시지 수정
    @PatchMapping("/{groupId}/motto")
    public ResponseEntity<String> updateMotto(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long groupId,
            @Valid @RequestBody MottoRequest req
    ) {
        groupService.updateMotto(securityUser.getMember(), groupId, req.motto());
        return ResponseEntity.ok("성공적으로 수정되었습니다.");
    }
}