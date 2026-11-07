package com.ll.demo.domain.push.event;

// 인앱 알림이 저장된 뒤 발행. 커밋 후 푸시로 보낸다
public record NotificationCreatedEvent(
        Long notificationId,
        Long receiverId,
        String type,
        String message,
        Long targetId
) { }
