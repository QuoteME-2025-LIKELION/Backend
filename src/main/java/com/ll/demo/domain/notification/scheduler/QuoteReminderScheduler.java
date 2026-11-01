package com.ll.demo.domain.notification.scheduler;

import com.ll.demo.domain.notification.entity.NotificationSetting;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import com.ll.demo.domain.notification.service.QuoteReminderSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteReminderScheduler {

    private static final int WINDOW_MINUTES = 5;

    private final NotificationSettingRepository notificationSettingRepository;
    private final QuoteReminderSender quoteReminderSender;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void sendQuoteReminders() {
        LocalDateTime now = LocalDateTime.now(NotificationSetting.REMINDER_ZONE);
        LocalDate today = now.toLocalDate();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        LocalTime windowEnd = now.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        LocalTime windowStart = windowEnd.minusMinutes(WINDOW_MINUTES - 1);
        if (windowStart.isAfter(windowEnd)) {
            windowStart = LocalTime.MIDNIGHT;
        }

        LocalTime defaultTime = NotificationSetting.DEFAULT_QUOTE_REMINDER_TIME;
        boolean includeDefault = !defaultTime.isBefore(windowStart) && !defaultTime.isAfter(windowEnd);

        List<Long> settingIds = notificationSettingRepository.findQuoteReminderTargetSettingIds(
                today, windowStart, windowEnd, includeDefault, dayStart, dayEnd);
        for (Long settingId : settingIds) {
            try {
                quoteReminderSender.sendForSetting(settingId, today);
            } catch (Exception e) {
                log.error("명언 작성 알림 발송 실패 settingId={}", settingId, e);
            }
        }

        if (includeDefault) {
            List<Long> memberIds = notificationSettingRepository.findMemberIdsWithoutSettingAndQuote(dayStart, dayEnd);
            for (Long memberId : memberIds) {
                try {
                    quoteReminderSender.sendForMemberWithoutSetting(memberId, today);
                } catch (Exception e) {
                    log.error("명언 작성 알림 발송 실패 memberId={}", memberId, e);
                }
            }
        }
    }
}
