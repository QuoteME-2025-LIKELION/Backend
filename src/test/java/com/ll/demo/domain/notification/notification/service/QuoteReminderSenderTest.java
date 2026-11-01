package com.ll.demo.domain.notification.notification.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.entity.NotificationSetting;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.notification.service.QuoteReminderSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteReminderSenderTest {

    @Mock private NotificationSettingRepository notificationSettingRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private QuoteReminderSender sender;

    private Member member;
    private final LocalDate today = LocalDate.of(2026, 10, 17);

    @BeforeEach
    void setUp() {
        member = Member.builder().nickname("수신자").build();
        ReflectionTestUtils.setField(member, "id", 1L);
    }

    @Test
    @DisplayName("성공: 선점에 성공하면 발신자 없는 QUOTE_REMINDER 알림을 만든다")
    void sendForSetting_claimed_createsNotification() {
        NotificationSetting setting = NotificationSetting.builder().member(member).build();
        when(notificationSettingRepository.claimQuoteReminder(10L, today)).thenReturn(1);
        when(notificationSettingRepository.findById(10L)).thenReturn(Optional.of(setting));

        sender.sendForSetting(10L, today);

        verify(notificationService).create(eq(member), isNull(), eq("QUOTE_REMINDER"), anyString(), isNull());
    }

    @Test
    @DisplayName("성공: 이미 선점된 설정(오늘 발송됨)은 알림을 만들지 않는다")
    void sendForSetting_alreadyClaimed_doesNothing() {
        when(notificationSettingRepository.claimQuoteReminder(10L, today)).thenReturn(0);

        sender.sendForSetting(10L, today);

        verify(notificationService, never()).create(any(), any(), any(), any(), any());
        verify(notificationSettingRepository, never()).findById(any());
    }

    @Test
    @DisplayName("성공: 설정 row가 없는 회원은 오늘 날짜가 기록된 기본 설정이 만들어지고 알림이 생성된다")
    void sendForMemberWithoutSetting_createsRowAndNotification() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(notificationSettingRepository.findByMemberId(1L)).thenReturn(Optional.empty());

        sender.sendForMemberWithoutSetting(1L, today);

        ArgumentCaptor<NotificationSetting> captor = ArgumentCaptor.forClass(NotificationSetting.class);
        verify(notificationSettingRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getMember()).isSameAs(member);
        assertThat(captor.getValue().getLastQuoteReminderDate()).isEqualTo(today);
        verify(notificationService).create(eq(member), isNull(), eq("QUOTE_REMINDER"), anyString(), isNull());
    }

    @Test
    @DisplayName("성공: 그 사이 설정 row가 생겼다면 아무것도 하지 않는다")
    void sendForMemberWithoutSetting_rowAppeared_doesNothing() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(notificationSettingRepository.findByMemberId(1L))
                .thenReturn(Optional.of(NotificationSetting.builder().member(member).build()));

        sender.sendForMemberWithoutSetting(1L, today);

        verify(notificationSettingRepository, never()).saveAndFlush(any());
        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }
}
