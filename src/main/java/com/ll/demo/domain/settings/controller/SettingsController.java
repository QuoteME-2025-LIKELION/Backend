package com.ll.demo.domain.settings.controller;

import com.ll.demo.domain.member.member.dto.FriendResponse;
import com.ll.demo.domain.member.member.dto.ProfileResponse;
import com.ll.demo.domain.member.member.dto.ProfileUpdateRequest;
import com.ll.demo.domain.member.member.dto.SearchCombinedResponse;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.aws.S3Service;
import com.ll.demo.global.security.SecurityUser;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final MemberService memberService;
    private final S3Service s3Service; // ★ 1. S3 서비스 주입

    // 내 프로필 조회
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(
            @AuthenticationPrincipal SecurityUser securityUser // SecurityUser로 변경
    ) {
        if (securityUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Long memberId = securityUser.getMember().getId();
        ProfileResponse response = memberService.getProfile(memberId);

        return ResponseEntity.ok(response);
    }

    // 프로필 수정
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // 멀티파트 요청 허용
    public ResponseEntity<Void> updateProfile(
            // JSON 데이터는 'data'라는 이름으로
            @RequestPart(value = "data") ProfileUpdateRequest request,
            // 이미지 파일은 'image'라는 이름으로
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal SecurityUser securityUser
    ) throws IOException { // 파일 업로드 실패 시 예외 처리

        Long memberId = securityUser.getMember().getId();
        String imageUrl = null;

        // 이미지가 전송되었다면 S3에 업로드하고 URL 받기
        if (image != null && !image.isEmpty()) {
            imageUrl = s3Service.uploadFile(image);
        }

        // 서비스에 수정 요청
        memberService.updateProfile(memberId, request, imageUrl);

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