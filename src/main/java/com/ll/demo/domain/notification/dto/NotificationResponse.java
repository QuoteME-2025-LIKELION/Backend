package com.ll.demo.domain.notification.dto;

import com.ll.demo.domain.notification.entity.Notification;

public record NotificationResponse(
        Long id,
        String type,
        String message,
        Long targetId,
        String senderName,
        String createDate,
        boolean isRead

) {
    public NotificationResponse(Notification n) {
        this(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getTargetId(),
                n.getSender().getName(), // ★ 여기서 DB 조회가 필요함
                n.getCreateDate().toString(),
                n.getReadDate() != null
        );
    }
}