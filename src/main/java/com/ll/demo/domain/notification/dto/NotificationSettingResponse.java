package com.ll.demo.domain.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ll.demo.domain.notification.entity.NotificationSetting;

import java.time.LocalTime;

public record NotificationSettingResponse(
        boolean groupEnabled,
        boolean friendEnabled,
        boolean tagEnabled,
        boolean pokeEnabled,
        boolean likeEnabled,
        boolean quoteReminderEnabled,
        boolean marketingEnabled,
        @JsonFormat(pattern = "HH:mm") LocalTime quoteReminderTime
) {
    public static NotificationSettingResponse from(NotificationSetting s) {
        return new NotificationSettingResponse(
                s.isGroupEnabled(),
                s.isFriendEnabled(),
                s.isTagEnabled(),
                s.isPokeEnabled(),
                s.isLikeEnabled(),
                s.isQuoteReminderEnabled(),
                s.isMarketingEnabled(),
                s.effectiveQuoteReminderTime()
        );
    }

    // 기본값 (설정이 없을 때)
    public static NotificationSettingResponse defaults() {
        return new NotificationSettingResponse(true, true, true, true, true, true, false,
                NotificationSetting.DEFAULT_QUOTE_REMINDER_TIME);
    }
}
