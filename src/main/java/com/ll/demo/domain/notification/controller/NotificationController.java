package com.ll.demo.domain.notification.controller;

import com.ll.demo.domain.notification.dto.NotificationResponse;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.global.security.SecurityUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 내 알림 목록 조회 (통합 & 필터링)
    // 1. 전체 보기: GET /api/notifications
    // 2. 콕 찌르기만 보기: GET /api/notifications?type=POKE
    // 3. 그룹 알림만 보기: GET /api/notifications?type=GROUP
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(required = false) String type // [추가] 필수 아님(false)
    ) {
        List<NotificationResponse> result = notificationService.findMyNotifications(user.getMember().getId(), type);
        return ResponseEntity.ok(result);
    }

    // 알림 읽음 처리 (PATCH)
    // PATCH /api/notifications/{id}/read
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> readNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user
    ) {
        // 서비스에 '읽음 처리' 메서드 만들어서 호출 필요
        notificationService.markAsRead(user.getMember().getId(), id);
        return ResponseEntity.ok().build();
    }



}