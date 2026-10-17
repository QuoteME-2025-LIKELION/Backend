package com.ll.demo.domain.notification.service;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.notification.dto.NotificationResponse;
import com.ll.demo.domain.notification.dto.NotificationSettingRequest;
import com.ll.demo.domain.notification.dto.NotificationSettingResponse;
import com.ll.demo.domain.notification.entity.Notification;
import com.ll.demo.domain.notification.entity.NotificationSetting;
import com.ll.demo.domain.notification.repository.NotificationRepository;
import com.ll.demo.domain.notification.repository.NotificationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    // 알림 타입 → 카테고리 매핑
    private static final Map<String, List<String>> CATEGORY_TYPES = Map.of(
            "GROUP",  List.of("GROUP_INVITE", "GROUP_ACCEPT", "GROUP_REJECT", "GROUP_LEAVE", "GROUP_MOTTO"),
            "FRIEND", List.of("FRIEND_REQUEST", "FRIEND_ACCEPTED", "FRIEND_REJECTED", "POKE"),
            "TAG",    List.of("TAG", "TAG_REQUEST", "TAG_ACCEPTED")
    );

    // 알림 타입 → 설정 카테고리 매핑
    private boolean isEnabled(NotificationSetting setting, String type) {
        return switch (type) {
            case "GROUP_INVITE", "GROUP_ACCEPT", "GROUP_REJECT", "GROUP_LEAVE", "GROUP_MOTTO" -> setting.isGroupEnabled();
            case "TAG", "TAG_REQUEST", "TAG_ACCEPTED" -> setting.isTagEnabled();
            case "POKE" -> setting.isPokeEnabled();
            case "LIKE" -> setting.isLikeEnabled();
            case "QUOTE_REMINDER" -> setting.isQuoteReminderEnabled();
            case "FRIEND_REQUEST", "FRIEND_ACCEPTED", "FRIEND_REJECTED" -> setting.isFriendEnabled();
            default -> true;
        };
    }

    @Transactional
    public void create(Member receiver, Member sender, String type, String message, Long targetId) {
        // 수신자의 알림 설정 확인: 해당 카테고리가 off면 저장하지 않음
        boolean blocked = notificationSettingRepository.findByMember(receiver)
                .map(setting -> !isEnabled(setting, type))
                .orElse(false);
        if (blocked) return;

        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .type(type)
                .message(message)
                .targetId(targetId)
                .build();

        notificationRepository.save(notification);
    }

    // 알림 목록 조회 (전체 or 카테고리 or 특정 타입)
    public List<NotificationResponse> findMyNotifications(Long memberId, String category) {
        List<Notification> notifications;

        if (category == null || category.isBlank()) {
            // 전체 조회
            notifications = notificationRepository.findByReceiverIdOrderByCreateDateDesc(memberId);
        } else if (CATEGORY_TYPES.containsKey(category.toUpperCase())) {
            // 카테고리 기반 조회 (GROUP, FRIEND, TAG)
            List<String> types = CATEGORY_TYPES.get(category.toUpperCase());
            notifications = notificationRepository.findByReceiverIdAndTypeInOrderByCreateDateDesc(memberId, types);
        } else {
            // 특정 타입 정확 조회 (하위 호환)
            notifications = notificationRepository.findByReceiverIdAndTypeOrderByCreateDateDesc(memberId, category);
        }

        return notifications.stream()
                .map(NotificationResponse::new)
                .toList();
    }

    @Transactional
    public void markAsRead(Long receiverId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));

        if (!notification.getReceiver().getId().equals(receiverId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        notification.markAsRead();
    }

    // 전체 읽지 않은 알림 개수 (홈 화면 뱃지용)
    public long countUnread(Long memberId) {
        return notificationRepository.countByReceiverIdAndReadDateIsNull(memberId);
    }

    // 읽지 않은 콕 찌르기 개수
    public long countUnreadByType(Long memberId, String type) {
        return notificationRepository.countByReceiverIdAndTypeAndReadDateIsNull(memberId, type);
    }

    // 알림 설정 조회 (없으면 기본값 반환)
    public NotificationSettingResponse getSetting(Member member) {
        return notificationSettingRepository.findByMember(member)
                .map(NotificationSettingResponse::from)
                .orElse(NotificationSettingResponse.defaults());
    }

    // 알림 설정 저장/수정
    @Transactional
    public NotificationSettingResponse updateSetting(Member member, NotificationSettingRequest req) {
        NotificationSetting setting = notificationSettingRepository.findByMember(member)
                .orElse(NotificationSetting.builder().member(member).build());

        LocalDateTime now = LocalDateTime.now(NotificationSetting.REMINDER_ZONE);
        LocalTime previousReminderTime = setting.effectiveQuoteReminderTime();
        boolean wasReminderEnabled = setting.isQuoteReminderEnabled();

        setting.setGroupEnabled(req.groupEnabled());
        setting.setFriendEnabled(req.friendEnabled());
        setting.setTagEnabled(req.tagEnabled());
        setting.setPokeEnabled(req.pokeEnabled());
        setting.setLikeEnabled(req.likeEnabled());
        setting.setQuoteReminderEnabled(req.quoteReminderEnabled());
        setting.setMarketingEnabled(req.marketingEnabled());

        if (req.quoteReminderTime() != null) {
            setting.setQuoteReminderTime(req.quoteReminderTime());
        }

        LocalTime reminderTime = setting.effectiveQuoteReminderTime();
        boolean timeChanged = !previousReminderTime.equals(reminderTime);
        boolean reEnabled = !wasReminderEnabled && setting.isQuoteReminderEnabled();
        if ((timeChanged || reEnabled) && !reminderTime.isAfter(now.toLocalTime())) {
            setting.setLastQuoteReminderDate(now.toLocalDate());
        }

        notificationSettingRepository.save(setting);
        return NotificationSettingResponse.from(setting);
    }
}
