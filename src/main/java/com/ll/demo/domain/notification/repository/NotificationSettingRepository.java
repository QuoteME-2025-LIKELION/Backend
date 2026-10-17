package com.ll.demo.domain.notification.repository;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByMember(Member member);
    Optional<NotificationSetting> findByMemberId(Long memberId);
    void deleteByMember(Member member);

    @Query("""
            SELECT s.id FROM NotificationSetting s
            WHERE s.quoteReminderEnabled = true
              AND (s.lastQuoteReminderDate IS NULL OR s.lastQuoteReminderDate < :today)
              AND (s.quoteReminderTime BETWEEN :windowStart AND :windowEnd
                   OR (s.quoteReminderTime IS NULL AND :includeDefault = true))
              AND NOT EXISTS (
                  SELECT 1 FROM Quote q
                  WHERE q.author = s.member
                    AND q.createDate >= :dayStart AND q.createDate < :dayEnd)
            """)
    List<Long> findQuoteReminderTargetSettingIds(
            @Param("today") LocalDate today,
            @Param("windowStart") LocalTime windowStart,
            @Param("windowEnd") LocalTime windowEnd,
            @Param("includeDefault") boolean includeDefault,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    @Query("""
            SELECT m.id FROM Member m
            WHERE NOT EXISTS (SELECT 1 FROM NotificationSetting s WHERE s.member = m)
              AND NOT EXISTS (
                  SELECT 1 FROM Quote q
                  WHERE q.author = m
                    AND q.createDate >= :dayStart AND q.createDate < :dayEnd)
            """)
    List<Long> findMemberIdsWithoutSettingAndQuote(
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    @Modifying
    @Query("""
            UPDATE NotificationSetting s SET s.lastQuoteReminderDate = :today
            WHERE s.id = :id
              AND (s.lastQuoteReminderDate IS NULL OR s.lastQuoteReminderDate < :today)
            """)
    int claimQuoteReminder(@Param("id") Long id, @Param("today") LocalDate today);
}
