package com.ll.demo.domain.notification.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.notification.dto.NotificationResponse;
import com.ll.demo.domain.notification.entity.Notification;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public void create(Member receiver, Member sender, String type, String message, Long targetId) {
        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .type(type)
                .message(message)
                .targetId(targetId)
                .build();

        notificationRepository.save(notification);
    }

    public List<NotificationResponse> findMyNotifications(Long memberId) {
        List<Notification> notifications = notificationRepository.findByReceiverIdOrderByCreateDateDesc(memberId);

        // ★ Service 내부(Transactional 안)에서 변환 수행
        return notifications.stream()
                .map(NotificationResponse::new)
                .toList();
    }

    @Transactional
    public void markAsRead(Long receiverId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));

        // 본인 알림인지 확인 (보안)
        if (!notification.getReceiver().getId().equals(receiverId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        notification.markAsRead(); // 엔티티의 readDate 갱신
    }

    // 매개변수에 type 추가 (null일 수도 있음)
    public List<NotificationResponse> findMyNotifications(Long memberId, String type) {
        List<Notification> notifications;

        if (type != null && !type.isBlank()) {
            // 타입이 지정되었으면 그것만 가져옴 (예: POKE)
            notifications = notificationRepository.findByReceiverIdAndTypeOrderByCreateDateDesc(memberId, type);
        } else {
            // 타입이 없으면 전체 다 가져옴
            notifications = notificationRepository.findByReceiverIdOrderByCreateDateDesc(memberId);
        }

        return notifications.stream()
                .map(NotificationResponse::new)
                .toList();
    }
}