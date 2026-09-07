package com.ll.demo.domain.notification.dto;

import com.ll.demo.domain.notification.entity.NotificationSetting;

public record NotificationSettingResponse(
        boolean groupEnabled,
        boolean friendEnabled,
        boolean tagEnabled,
        boolean pokeEnabled,
        boolean likeEnabled,
        boolean quoteReminderEnabled,
        boolean marketingEnabled
) {
    public static NotificationSettingResponse from(NotificationSetting s) {
        return new NotificationSettingResponse(
                s.isGroupEnabled(),
                s.isFriendEnabled(),
                s.isTagEnabled(),
                s.isPokeEnabled(),
                s.isLikeEnabled(),
                s.isQuoteReminderEnabled(),
                s.isMarketingEnabled()
        );
    }

    // 기본값 (설정이 없을 때)
    public static NotificationSettingResponse defaults() {
        return new NotificationSettingResponse(true, true, true, true, true, true, false);
    }
}
