package com.ll.demo.domain.notification.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.entity.NotificationSetting;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuoteReminderSender {

    public static final String TYPE = "QUOTE_REMINDER";
    private static final String MESSAGE = "오늘의 명언을 작성할 시간이에요!";

    private final NotificationSettingRepository notificationSettingRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Transactional
    public void sendForSetting(Long settingId, LocalDate today) {
        if (notificationSettingRepository.claimQuoteReminder(settingId, today) == 0) {
            return;
        }
        NotificationSetting setting = notificationSettingRepository.findById(settingId)
                .orElseThrow(() -> new IllegalStateException("알림 설정을 찾을 수 없습니다. id=" + settingId));
        notificationService.create(setting.getMember(), null, TYPE, MESSAGE, null);
    }

    @Transactional
    public void sendForMemberWithoutSetting(Long memberId, LocalDate today) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null || notificationSettingRepository.findByMemberId(memberId).isPresent()) {
            return;
        }
        notificationSettingRepository.saveAndFlush(
                NotificationSetting.builder().member(member).lastQuoteReminderDate(today).build());
        notificationService.create(member, null, TYPE, MESSAGE, null);
    }
}
