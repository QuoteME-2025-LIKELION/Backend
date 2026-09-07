package com.ll.demo.domain.notification.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByMember(Member member);
    Optional<NotificationSetting> findByMemberId(Long memberId);
}
