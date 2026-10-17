package com.ll.demo.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record NotificationSettingRequest(
        boolean groupEnabled,
        boolean friendEnabled,
        boolean tagEnabled,
        boolean pokeEnabled,
        boolean likeEnabled,
        boolean quoteReminderEnabled,
        boolean marketingEnabled,
        @JsonFormat(pattern = "HH:mm") LocalTime quoteReminderTime
) {}
