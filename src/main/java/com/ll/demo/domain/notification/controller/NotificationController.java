package com.ll.demo.domain.notification.controller;

import com.ll.demo.domain.notification.dto.NotificationResponse;
import com.ll.demo.domain.notification.dto.NotificationSettingRequest;
import com.ll.demo.domain.notification.dto.NotificationSettingResponse;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.global.security.SecurityUser;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 내 알림 목록 조회 (전체 & 카테고리 필터링)
    // GET /api/notifications           → 전체보기
    // GET /api/notifications?category=GROUP   → 그룹 알림
    // GET /api/notifications?category=FRIEND  → 친구 알림
    // GET /api/notifications?category=TAG     → 태그 알림
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(required = false) String category
    ) {
        List<NotificationResponse> result = notificationService.findMyNotifications(user.getMember().getId(), category);
        return ResponseEntity.ok(result);
    }

    // 미읽음 알림 개수 (홈 화면 뱃지용)
    // GET /api/notifications/unread-count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal SecurityUser user
    ) {
        long count = notificationService.countUnread(user.getMember().getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    // 알림 읽음 처리
    // PATCH /api/notifications/{id}/read
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> readNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser user
    ) {
        notificationService.markAsRead(user.getMember().getId(), id);
        return ResponseEntity.ok().build();
    }

    // 알림 설정 조회
    // GET /api/notifications/settings
    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingResponse> getSettings(
            @AuthenticationPrincipal SecurityUser user
    ) {
        return ResponseEntity.ok(notificationService.getSetting(user.getMember()));
    }

    // 알림 설정 저장/수정
    // PUT /api/notifications/settings
    @PutMapping("/settings")
    public ResponseEntity<NotificationSettingResponse> updateSettings(
            @AuthenticationPrincipal SecurityUser user,
            @RequestBody NotificationSettingRequest req
    ) {
        return ResponseEntity.ok(notificationService.updateSetting(user.getMember(), req));
    }
}
