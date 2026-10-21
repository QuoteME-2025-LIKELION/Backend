package com.ll.demo.domain.notification.notification.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.dto.NotificationResponse;
import com.ll.demo.domain.notification.dto.NotificationSettingRequest;
import com.ll.demo.domain.notification.dto.NotificationSettingResponse;
import com.ll.demo.domain.notification.entity.Notification;
import com.ll.demo.domain.notification.entity.NotificationSetting;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import com.ll.demo.domain.quote.entity.Quote;
import com.ll.demo.domain.quote.repository.QuoteRepository;

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
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Mock private MemberRepository memberRepository;

    @Mock private NotificationSettingRepository notificationSettingRepository;

    @Mock private QuoteRepository quoteRepository;

    private Member receiver;
    private Member sender;

    @BeforeEach
    void setUp() {
        receiver = Member.builder().nickname("수신자").build();
        ReflectionTestUtils.setField(receiver, "id", 1L);

        sender = Member.builder().nickname("발신자").build();
        ReflectionTestUtils.setField(sender, "id", 2L);
    }

    @Test
    @DisplayName("성공: 알림 조회 시 각 타입에 맞는 targetId가 반환되어야 한다")
    void findMyNotifications_DataIntegrity() {
        // given
        Notification pokeNote = Notification.builder()
                .type("POKE").targetId(2L).receiver(receiver).sender(sender).message("콕!").build();
        ReflectionTestUtils.setField(pokeNote, "createDate", java.time.LocalDateTime.now());

        Notification likeNote = Notification.builder()
                .type("LIKE").targetId(100L).receiver(receiver).sender(sender).message("좋아요!").build();
        ReflectionTestUtils.setField(likeNote, "createDate", java.time.LocalDateTime.now());

        when(notificationRepository.findByReceiverIdOrderByCreateDateDesc(1L))
                .thenReturn(List.of(pokeNote, likeNote));

        // when
        List<NotificationResponse> result = notificationService.findMyNotifications(1L, null);

        // then
        assertThat(result.get(0).type()).isEqualTo("POKE");
        assertThat(result.get(0).targetId()).isEqualTo(2L); // 찌른 사람 ID 확인

        assertThat(result.get(1).type()).isEqualTo("LIKE");
        assertThat(result.get(1).targetId()).isEqualTo(100L); // 명언 ID 확인
    }

    @Test
    @DisplayName("성공: 읽음 처리 시 readDate가 생성되고 isRead가 true가 되어야 한다")
    void markAsRead_Success() {
        // given
        Notification note;
        note = Notification.builder().receiver(receiver).sender(sender).type("POKE").message("콕!").targetId(2L).build();
        ReflectionTestUtils.setField(note, "id", 50L);
        ReflectionTestUtils.setField(note, "createDate", java.time.LocalDateTime.now());

        when(notificationRepository.findById(50L)).thenReturn(Optional.of(note));

        // when
        notificationService.markAsRead(1L, 50L);

        // then
        assertThat(note.getReadDate()).isNotNull(); // 읽은 시간 기록 확인

        // DTO 변환 시 true인지 확인
        NotificationResponse response = new NotificationResponse(note);
        assertThat(response.isRead()).isTrue(); // readDate != null 이면 true
    }

    @Test
    @DisplayName("성공: 특정 타입을 지정하면 해당 알림만 필터링되어야 한다")
    void findMyNotifications_WithFilter() {

        Long memberId = 1L;
        String type = "POKE";

        Notification realNotification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .type(type)
                .message("콕!")
                .targetId(2L)
                .build();
        ReflectionTestUtils.setField(realNotification, "createDate", java.time.LocalDateTime.now());

        when(notificationRepository.findByReceiverIdAndTypeOrderByCreateDateDesc(eq(memberId), eq(type)))
                .thenReturn(List.of(realNotification));

        List<NotificationResponse> result = notificationService.findMyNotifications(memberId, type);

        verify(notificationRepository, times(1)).findByReceiverIdAndTypeOrderByCreateDateDesc(memberId, type);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).senderName()).isEqualTo("발신자");
    }

    @Test
    @DisplayName("성공: 197번 알림을 읽음 처리하면 isRead가 true가 되어야 한다")
    void markAsRead_197_Success() {
        Long receiverId = 2L;
        Long notificationId = 197L;

        Member receiver = Member.builder().build();
        ReflectionTestUtils.setField(receiver, "id", receiverId);

        Member sender = Member.builder().nickname("발신자").build();
        ReflectionTestUtils.setField(sender, "id", 99L);

        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .type("TAG_ACCEPTED")
                .message("a님이 태그 요청을 수락했습니다!")
                .build();
        ReflectionTestUtils.setField(notification, "id", notificationId);
        ReflectionTestUtils.setField(notification, "createDate", java.time.LocalDateTime.now());

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(receiverId, notificationId);

        assertThat(notification.getReadDate()).isNotNull();

        NotificationResponse response = new NotificationResponse(notification);
        assertThat(response.isRead()).isTrue(); // 읽음 상태가 true?
    }

    private NotificationSettingRequest settingRequest(boolean quoteReminderEnabled, LocalTime time) {
        return new NotificationSettingRequest(true, true, true, true, true, quoteReminderEnabled, false, time);
    }

    private LocalDate todayKst() {
        return LocalDate.now(NotificationSetting.REMINDER_ZONE);
    }

    @Test
    @DisplayName("성공: 설정 row가 없을 때 시간을 저장하면 저장값과 응답에 시간이 담긴다")
    void updateSetting_newRow_savesTime() {
        when(notificationSettingRepository.findByMember(receiver)).thenReturn(Optional.empty());

        NotificationSettingResponse res = notificationService.updateSetting(receiver, settingRequest(true, LocalTime.of(21, 0)));

        ArgumentCaptor<NotificationSetting> captor = ArgumentCaptor.forClass(NotificationSetting.class);
        verify(notificationSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getQuoteReminderTime()).isEqualTo(LocalTime.of(21, 0));
        assertThat(res.quoteReminderTime()).isEqualTo(LocalTime.of(21, 0));
    }

    @Test
    @DisplayName("성공: 요청에서 시간을 생략하면 기존 시간이 유지된다")
    void updateSetting_omittedTime_keepsExisting() {
        NotificationSetting existing = NotificationSetting.builder()
                .member(receiver).quoteReminderTime(LocalTime.of(20, 0)).build();
        when(notificationSettingRepository.findByMember(receiver)).thenReturn(Optional.of(existing));

        NotificationSettingResponse res = notificationService.updateSetting(receiver, settingRequest(true, null));

        assertThat(existing.getQuoteReminderTime()).isEqualTo(LocalTime.of(20, 0));
        assertThat(existing.getLastQuoteReminderDate()).isNull();
        assertThat(res.quoteReminderTime()).isEqualTo(LocalTime.of(20, 0));
    }

    @Test
    @DisplayName("성공: 이미 지난 시각으로 변경하면 오늘은 발송하지 않도록 오늘 날짜가 기록된다")
    void updateSetting_changeToPastTime_suppressesToday() {
        NotificationSetting existing = NotificationSetting.builder()
                .member(receiver).quoteReminderTime(LocalTime.of(10, 0)).build();
        when(notificationSettingRepository.findByMember(receiver)).thenReturn(Optional.of(existing));

        notificationService.updateSetting(receiver, settingRequest(true, LocalTime.MIDNIGHT));

        assertThat(existing.getLastQuoteReminderDate()).isEqualTo(todayKst());
    }

    @Test
    @DisplayName("성공: 아직 지나지 않은 시각으로 변경하면 오늘 발송 가능 상태가 유지된다")
    void updateSetting_changeToFutureTime_keepsSendable() {
        assumeTrue(LocalTime.now(NotificationSetting.REMINDER_ZONE).isBefore(LocalTime.of(23, 58)));
        NotificationSetting existing = NotificationSetting.builder()
                .member(receiver).quoteReminderTime(LocalTime.of(10, 0)).build();
        when(notificationSettingRepository.findByMember(receiver)).thenReturn(Optional.of(existing));

        notificationService.updateSetting(receiver, settingRequest(true, LocalTime.of(23, 59)));

        assertThat(existing.getLastQuoteReminderDate()).isNull();
    }

    @Test
    @DisplayName("성공: 알림을 껐다가 켜면, 알림 시간이 이미 지났을 때 오늘은 발송하지 않는다")
    void updateSetting_reEnable_pastTime_suppressesToday() {
        NotificationSetting existing = NotificationSetting.builder()
                .member(receiver).quoteReminderEnabled(false).quoteReminderTime(LocalTime.MIDNIGHT).build();
        when(notificationSettingRepository.findByMember(receiver)).thenReturn(Optional.of(existing));

        notificationService.updateSetting(receiver, settingRequest(true, null));

        assertThat(existing.getLastQuoteReminderDate()).isEqualTo(todayKst());
    }

    @Test
    @DisplayName("성공: 설정 row가 없으면 기본 시간 10:00이 응답된다")
    void getSetting_noRow_returnsDefaultTime() {
        when(notificationSettingRepository.findByMember(receiver)).thenReturn(Optional.empty());

        assertThat(notificationService.getSetting(receiver).quoteReminderTime()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("성공: 발신자가 없는 알림도 NotificationResponse로 변환된다")
    void notificationResponse_nullSender_doesNotThrow() {
        Notification reminder = Notification.builder()
                .receiver(receiver).type("QUOTE_REMINDER").message("오늘의 명언을 작성할 시간이에요!").build();
        ReflectionTestUtils.setField(reminder, "createDate", java.time.LocalDateTime.now());

        NotificationResponse response = new NotificationResponse(reminder);

        assertThat(response.senderName()).isNull();
        assertThat(response.type()).isEqualTo("QUOTE_REMINDER");
    }

    private Notification notificationOf(String type, Long targetId) {
        Notification notification = Notification.builder()
                .receiver(receiver).sender(sender).type(type).message("메시지").targetId(targetId).build();
        ReflectionTestUtils.setField(notification, "createDate", java.time.LocalDateTime.of(2026, 10, 21, 22, 0));
        return notification;
    }

    private Quote quoteOf(Long id, java.time.LocalDateTime createDate) {
        Quote quote = Quote.builder().author(sender).content("명언").build();
        ReflectionTestUtils.setField(quote, "id", id);
        ReflectionTestUtils.setField(quote, "createDate", createDate);
        return quote;
    }

    @Test
    @DisplayName("성공: TAG 계열 알림에는 알림 생성일이 아닌 대상 명언의 작성일이 담긴다")
    void findMyNotifications_tagTypes_includeQuoteDate() {
        Quote quote = quoteOf(100L, java.time.LocalDateTime.of(2026, 10, 1, 9, 0));
        when(notificationRepository.findByReceiverIdOrderByCreateDateDesc(1L)).thenReturn(List.of(
                notificationOf("TAG", 100L),
                notificationOf("TAG_REQUEST", 100L),
                notificationOf("TAG_ACCEPTED", 100L)));
        when(quoteRepository.findAllById(anyIterable())).thenReturn(List.of(quote));

        List<NotificationResponse> result = notificationService.findMyNotifications(1L, null);

        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(response -> {
            assertThat(response.quoteDate()).isEqualTo("2026-10-01");
            assertThat(response.createDate()).startsWith("2026-10-21");
        });
    }

    @Test
    @DisplayName("성공: TAG 계열이 아닌 알림에는 targetId가 명언 ID와 같아도 작성일이 담기지 않는다")
    void findMyNotifications_nonTagTypes_haveNoQuoteDate() {
        Quote quote = quoteOf(100L, java.time.LocalDateTime.of(2026, 10, 1, 9, 0));
        when(notificationRepository.findByReceiverIdOrderByCreateDateDesc(1L)).thenReturn(List.of(
                notificationOf("TAG", 100L),
                notificationOf("LIKE", 100L),
                notificationOf("GROUP_MOTTO", 100L)));
        when(quoteRepository.findAllById(anyIterable())).thenReturn(List.of(quote));

        List<NotificationResponse> result = notificationService.findMyNotifications(1L, null);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).quoteDate()).isEqualTo("2026-10-01");
        assertThat(result.get(1).quoteDate()).isNull();
        assertThat(result.get(2).quoteDate()).isNull();
    }

    @Test
    @DisplayName("성공: 대상 명언이 없는 TAG 계열 알림은 목록에서 제외된다")
    void findMyNotifications_tagNotificationWithMissingQuote_isExcluded() {
        when(notificationRepository.findByReceiverIdOrderByCreateDateDesc(1L)).thenReturn(List.of(
                notificationOf("TAG", 999L),
                notificationOf("POKE", 2L)));
        when(quoteRepository.findAllById(anyIterable())).thenReturn(List.of());

        List<NotificationResponse> result = notificationService.findMyNotifications(1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("POKE");
    }

    @Test
    @DisplayName("성공: TAG 계열 알림이 없으면 명언을 조회하지 않는다")
    void findMyNotifications_withoutTagNotifications_doesNotQueryQuotes() {
        when(notificationRepository.findByReceiverIdOrderByCreateDateDesc(1L))
                .thenReturn(List.of(notificationOf("POKE", 2L)));

        notificationService.findMyNotifications(1L, null);

        verify(quoteRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("성공: GROUP 카테고리 조회에 가입 요청 알림이 포함된다")
    void findMyNotifications_groupCategory_includesJoinRequestType() {
        when(notificationRepository.findByReceiverIdAndTypeInOrderByCreateDateDesc(
                eq(1L), argThat((Collection<String> types) -> types.contains("GROUP_JOIN_REQUEST"))))
                .thenReturn(List.of(notificationOf("GROUP_JOIN_REQUEST", 10L)));

        List<NotificationResponse> result = notificationService.findMyNotifications(1L, "GROUP");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("GROUP_JOIN_REQUEST");
        assertThat(result.get(0).quoteDate()).isNull();
    }

    @Test
    @DisplayName("성공: 그룹 알림을 끈 수신자에게는 가입 요청 알림이 저장되지 않는다")
    void create_groupJoinRequest_respectsGroupSetting() {
        NotificationSetting off = NotificationSetting.builder().member(receiver).groupEnabled(false).build();
        when(notificationSettingRepository.findByMember(receiver)).thenReturn(Optional.of(off));

        notificationService.create(receiver, sender, "GROUP_JOIN_REQUEST", "메시지", 10L);

        verify(notificationRepository, never()).save(any());
    }
}