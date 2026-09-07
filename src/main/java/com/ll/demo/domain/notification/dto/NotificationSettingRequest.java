package com.ll.demo.domain.notification.dto;

public record NotificationSettingRequest(
        boolean groupEnabled,
        boolean friendEnabled,
        boolean tagEnabled,
        boolean pokeEnabled,
        boolean likeEnabled,
        boolean quoteReminderEnabled,
        boolean marketingEnabled
) {}
