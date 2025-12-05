package com.ll.demo.domain.notification.repository;

import com.ll.demo.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 1. 전체 알림 조회 (기존)
    List<Notification> findByReceiverIdOrderByCreateDateDesc(Long receiverId);

    // 2. [추가] 특정 타입의 알림만 조회 (예: POKE만, GROUP만)
    List<Notification> findByReceiverIdAndTypeOrderByCreateDateDesc(Long receiverId, String type);
}