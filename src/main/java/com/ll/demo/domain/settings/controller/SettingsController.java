package com.ll.demo.domain.settings.controller;

import com.ll.demo.domain.member.member.dto.ProfileResponse;
import com.ll.demo.domain.member.member.dto.ProfileUpdateRequest;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.ll.demo.domain.member.member.dto.FriendResponse;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.dto.SearchCombinedResponse; // 응답 dto 교체
import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final MemberService memberService;

    // 내 프로필 조회
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal Member member
    ) {
        Long memberId = member.getId();
        ProfileResponse response = memberService.getProfile(memberId);

        return ResponseEntity.ok(response);
    }

    // 프로필 수정
    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            // 프로필 이미지 포함 - @ModelAttribute 사용
            @ModelAttribute ProfileUpdateRequest request,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long memberId = securityUser.getMember().getId();
        memberService.updateProfile(memberId, request);
        return ResponseEntity.ok().build();
    }

    // 친구 및 그룹 통합검색
    @GetMapping("/search")
    public ResponseEntity<SearchCombinedResponse> searchMembers(
            @RequestParam(name = "keyword") String keyword,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long memberId = securityUser.getMember().getId();
        SearchCombinedResponse response = memberService.searchCombined(keyword, memberId);
        return ResponseEntity.ok(response);
    }

    // 친구 목록
    @GetMapping("/friends-list")
    public ResponseEntity<List<FriendResponse>> getFriendsAndGroups(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        if (securityUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "401-1. 로그인 인증 정보가 유효하지 않습니다."
            );
        }
        Long memberId = securityUser.getMember().getId();
        List<FriendResponse> response = memberService.getFriendList(memberId);
        return ResponseEntity.ok(response);
    }
}