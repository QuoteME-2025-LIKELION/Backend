package com.ll.demo.domain.notification.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 전체 알림 조회
    List<Notification> findByReceiverIdOrderByCreateDateDesc(Long receiverId);

    // 특정 타입의 알림만 조회
    List<Notification> findByReceiverIdAndTypeOrderByCreateDateDesc(Long receiverId, String type);

    // 카테고리(복수 타입) 알림 조회
    List<Notification> findByReceiverIdAndTypeInOrderByCreateDateDesc(Long receiverId, Collection<String> types);

    // 특정 타입의 읽지 않은 알림 개수
    long countByReceiverIdAndTypeAndReadDateIsNull(Long receiverId, String type);

    // 전체 읽지 않은 알림 개수
    long countByReceiverIdAndReadDateIsNull(Long receiverId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.receiver = :member OR n.sender = :member")
    void deleteAllByMember(@Param("member") Member member);
}