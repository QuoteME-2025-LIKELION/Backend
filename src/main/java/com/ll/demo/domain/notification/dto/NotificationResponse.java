package com.ll.demo.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ll.demo.domain.notification.entity.Notification;

import java.time.LocalDate;

public record NotificationResponse(
        Long id,
        String type,
        String message,
        Long targetId,
        String senderName,
        String createDate,
        boolean isRead,
        @JsonInclude(JsonInclude.Include.NON_NULL) String quoteDate
) {
    public NotificationResponse(Notification n) {
        this(n, null);
    }

    public NotificationResponse(Notification n, LocalDate quoteDate) {
        this(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getTargetId(),
                n.getSender() != null ? n.getSender().getName() : null,
                n.getCreateDate().toString(),
                n.getReadDate() != null,
                quoteDate != null ? quoteDate.toString() : null
        );
    }
}