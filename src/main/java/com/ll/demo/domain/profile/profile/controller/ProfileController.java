package com.ll.demo.domain.profile.profile.controller;

import com.ll.demo.domain.profile.profile.dto.AccountInfoResponse;
import com.ll.demo.domain.profile.profile.dto.AccountUpdateRequest;
import com.ll.demo.domain.profile.profile.dto.ProfileResponse;
import com.ll.demo.domain.profile.profile.dto.ProfileUpdateRequest;
import com.ll.demo.domain.profile.profile.service.ProfileService;
import com.ll.demo.global.aws.S3Service;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.security.SecurityUser;
import com.ll.demo.global.security.oauth2.OAuth2MemberService;
import com.ll.demo.standard.rq.Rq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private static final Set<String> LINKABLE_PROVIDERS = Set.of("google", "kakao");

    private final ProfileService profileService;
    private final S3Service s3Service;
    private final Rq rq;

    // 내 프로필 조회
    @GetMapping("")
    public ResponseEntity<ProfileResponse> getMyProfile(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        return ResponseEntity.ok(profileService.getMyProfile(securityUser.getMember()));
    }

    // 프로필 수정 (닉네임, 자기소개, 이미지)
    @PutMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestPart(value = "data", required = false) ProfileUpdateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) throws IOException {
        // 업로드 전에 차단해 S3에 고아 파일이 남지 않게 함
        profileService.validateNotGuest(securityUser.getMember());
        String imageUrl = (image != null && !image.isEmpty()) ? s3Service.uploadFile(image) : null;
        return ResponseEntity.ok(profileService.updateProfile(securityUser.getMember(), request, imageUrl));
    }

    // 계정 정보 조회 (성별, 생년월일, 소셜 연동 상태)
    @GetMapping("/account")
    public ResponseEntity<AccountInfoResponse> getAccountInfo(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        return ResponseEntity.ok(profileService.getAccountInfo(securityUser.getMember()));
    }

    // 계정 정보 수정 (성별, 생년월일)
    @PutMapping("/account")
    public ResponseEntity<String> updateAccount(
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody AccountUpdateRequest request
    ) {
        profileService.updateAccountInfo(securityUser.getMember(), request);
        return ResponseEntity.ok("계정 정보가 수정되었습니다.");
    }

    // 소셜 계정 연동 시작 (브라우저 이동으로 호출, 세션에 연동 대상을 남기고 OAuth로 이동)
    @GetMapping("/account/link/{provider}")
    public ResponseEntity<Void> startSocialLink(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable String provider,
            HttpServletRequest request
    ) {
        if (!LINKABLE_PROVIDERS.contains(provider)) {
            throw new GlobalException("400", "지원하지 않는 소셜 서비스입니다.");
        }
        profileService.validateNotGuest(securityUser.getMember());

        request.getSession().setAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE, securityUser.getMember().getId());
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/" + provider))
                .build();
    }

    // 계정 탈퇴
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        profileService.deleteAccount(securityUser.getMember());
        rq.deleteCookie("accessToken");
        rq.deleteCookie("refreshToken");
        return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
    }
}
