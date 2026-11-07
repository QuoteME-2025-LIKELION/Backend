package com.ll.demo.domain.push.controller;

import com.ll.demo.domain.push.dto.PushTokenDeleteRequest;
import com.ll.demo.domain.push.dto.PushTokenRequest;
import com.ll.demo.domain.push.service.PushTokenService;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push/tokens")
@RequiredArgsConstructor
public class PushTokenController {

    private final PushTokenService pushTokenService;

    // 기기 토큰 등록 (같은 토큰이면 소유자 이전)
    @PostMapping
    public ResponseEntity<RsData<String>> register(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody PushTokenRequest req
    ) {
        pushTokenService.register(user.getMember(), req.token(), req.platform());
        return ResponseEntity.ok(RsData.of("200", "푸시 토큰을 등록했습니다.", null));
    }

    // 기기 토큰 삭제 (로그아웃 시)
    @DeleteMapping
    public ResponseEntity<RsData<String>> unregister(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody PushTokenDeleteRequest req
    ) {
        pushTokenService.unregister(user.getMember(), req.token());
        return ResponseEntity.ok(RsData.of("200", "푸시 토큰을 삭제했습니다.", null));
    }
}
