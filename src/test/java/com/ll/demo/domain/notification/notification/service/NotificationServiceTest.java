package com.ll.demo.domain.notification.notification.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.notification.dto.NotificationResponse;
import com.ll.demo.domain.notification.entity.Notification;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Mock private MemberRepository memberRepository;

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
}