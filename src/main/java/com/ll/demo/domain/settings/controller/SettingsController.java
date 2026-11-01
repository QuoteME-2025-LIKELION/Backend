package com.ll.demo.domain.settings.controller;

import com.ll.demo.domain.member.member.dto.FriendResponse;
import com.ll.demo.domain.member.member.dto.SearchCombinedResponse;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final MemberService memberService;

    // 친구 및 그룹 통합 검색
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
    public ResponseEntity<List<FriendResponse>> getFriendsList(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long memberId = securityUser.getMember().getId();
        List<FriendResponse> response = memberService.getFriendList(memberId);
        return ResponseEntity.ok(response);
    }
}
