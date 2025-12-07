package com.ll.demo.domain.profile.profile.controller;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.profile.profile.dto.ProfileResponse;
import com.ll.demo.domain.profile.profile.dto.ProfileUpdateRequest;
import com.ll.demo.domain.profile.profile.service.ProfileService;
import com.ll.demo.global.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
}