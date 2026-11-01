package com.ll.demo.domain.group.group.controller;

import com.ll.demo.domain.group.group.dto.GroupDetailResponse;
import com.ll.demo.domain.group.group.dto.GroupInviteResponse;
import com.ll.demo.domain.group.group.dto.GroupJoinRequestResponse;
import com.ll.demo.domain.group.group.dto.GroupRequest;
import com.ll.demo.domain.group.group.dto.GroupResponse;
import com.ll.demo.domain.group.group.dto.MottoRequest;
import com.ll.demo.domain.group.group.service.GroupService;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.SecurityUser;
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
    public ResponseEntity<RsData<String>> invite(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long groupId,
            @PathVariable Long friendId
    ) {
        groupService.inviteFriend(user.getMember(), groupId, friendId);
        return ResponseEntity.ok(RsData.of("200", "초대가 완료되었습니다."));
    }

    // 내가 특정 그룹에 보낸 초대 대기 목록 조회 (그룹장 전용)
    @GetMapping("/{groupId}/invitations/sent")
    public ResponseEntity<List<GroupJoinRequestResponse>> getSentInvitations(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(groupService.getSentInvitations(user.getMember(), groupId));
    }

    // 특정 그룹에서 보낸 초대 취소 (그룹장 전용)
    @DeleteMapping("/{groupId}/invitations/{requestId}")
    public ResponseEntity<RsData<String>> cancelInvitation(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long groupId,
            @PathVariable Long requestId
    ) {
        groupService.cancelInvitation(user.getMember(), groupId, requestId);
        return ResponseEntity.ok(RsData.of("200", "그룹 초대를 취소했습니다."));
    }

    // 그룹 멤버 삭제 / 탈퇴
    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<RsData<String>> removeOrLeave(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long groupId,
            @PathVariable Long memberId
    ) {
        groupService.removeOrLeaveMember(user.getMember(), groupId, memberId);
        return ResponseEntity.ok(RsData.of("200", "완료되었습니다."));
    }

    // 그룹 가입 요청
    @PostMapping("/{groupId}/join-request")
    public ResponseEntity<RsData<String>> joinRequest(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long groupId
    ) {
        groupService.requestToJoin(user.getMember(), groupId);
        return ResponseEntity.ok(RsData.of("200", "참여 요청이 발송되었습니다."));
    }

    // 가입 요청 수락
    @PostMapping("/join-requests/{requestId}/accept")
    public ResponseEntity<RsData<String>> acceptRequest(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long requestId
    ) {
        groupService.acceptJoinRequest(user.getMember(), requestId);
        return ResponseEntity.ok(RsData.of("200", "가입 승인이 완료되었습니다."));
    }

    // 가입 요청 거절
    @PostMapping("/join-requests/{requestId}/reject")
    public ResponseEntity<RsData<String>> rejectRequest(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long requestId
    ) {
        groupService.rejectJoinRequest(user.getMember(), requestId);
        return ResponseEntity.ok(RsData.of("200", "가입 요청을 거절했습니다."));
    }

    // 그룹 가입 요청 목록 조회 (그룹장 전용)
    @GetMapping("/{groupId}/join-requests")
    public ResponseEntity<List<GroupJoinRequestResponse>> getJoinRequests(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long groupId
    ) {
        return ResponseEntity.ok(groupService.getJoinRequests(user.getMember(), groupId));
    }

    // 특정 그룹 상세 조회
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDetailResponse> getGroupDetail(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupDetail(groupId));
    }

    // 그룹 삭제
    @DeleteMapping("/{groupId}")
    public ResponseEntity<RsData<String>> deleteGroup(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long groupId
    ) {
        groupService.deleteGroup(user.getMember(), groupId);
        return ResponseEntity.ok(RsData.of("200", "그룹이 삭제되었습니다."));
    }

    // 그룹 메시지(좌우명) 수정
    @PatchMapping("/{groupId}/motto")
    public ResponseEntity<RsData<String>> updateMotto(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long groupId,
            @Valid @RequestBody MottoRequest req
    ) {
        groupService.updateMotto(securityUser.getMember(), groupId, req.motto());
        return ResponseEntity.ok(RsData.of("200", "성공적으로 수정되었습니다."));
    }

    // 내게 온 그룹 초대 목록 조회
    @GetMapping("/invitations")
    public ResponseEntity<List<GroupInviteResponse>> getMyInvitations(
            @AuthenticationPrincipal SecurityUser user
    ) {
        return ResponseEntity.ok(groupService.getMyInvitations(user.getMember()));
    }

    // 그룹 초대 수락
    @PostMapping("/invitations/{requestId}/accept")
    public ResponseEntity<RsData<String>> acceptInvitation(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long requestId
    ) {
        groupService.acceptInvitation(user.getMember(), requestId);
        return ResponseEntity.ok(RsData.of("200", "그룹 초대를 수락했습니다."));
    }

    // 그룹 초대 거절
    @PostMapping("/invitations/{requestId}/reject")
    public ResponseEntity<RsData<String>> rejectInvitation(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long requestId
    ) {
        groupService.rejectInvitation(user.getMember(), requestId);
        return ResponseEntity.ok(RsData.of("200", "그룹 초대를 거절했습니다."));
    }
}
