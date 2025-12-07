package com.ll.demo.domain.profile.profile.controller;

import com.ll.demo.domain.profile.profile.dto.AccountUpdateRequest;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.profile.profile.dto.ProfileResponse;
import com.ll.demo.domain.profile.profile.dto.ProfileUpdateRequest;
import com.ll.demo.domain.profile.profile.service.ProfileService;
import com.ll.demo.global.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @GetMapping("")
    public ResponseEntity<ProfileResponse> getMyProfile(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        return ResponseEntity.ok(profileService.getMyProfile(securityUser.getMember()));
    }

    @PostMapping("")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(profileService.updateProfile(securityUser.getMember(), request));
    }


    // 계정 정보 수정
    @PutMapping("/account")
    public ResponseEntity<String> updateAccount(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody AccountUpdateRequest request
    ) {
        profileService.updateAccountInfo(securityUser.getMember(), request);
        return ResponseEntity.ok("계정 정보가 수정되었습니다.");
    }

    // 계정 삭제
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        profileService.deleteAccount(securityUser.getMember());
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }
}