package com.ll.demo.domain.notification.entity;

import static lombok.AccessLevel.PROTECTED;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Notification extends BaseTime {

    @ManyToOne(fetch = FetchType.LAZY)
    private Member receiver; // 알림 받는 사람

    @ManyToOne(fetch = FetchType.LAZY)
    private Member sender;   // 알림 보낸 사람 (누가 찔렀는지 표시)

    private String type;     // 알림 타입 (예: "POKE", "LIKE")

    private String message;  // 알림 메시지 (예: "말랑이님이 콕 찔렀어요!")

    private LocalDateTime readDate; // 읽은 시간 (null이면 안 읽음)

    // 예: LIKE 알림이면 -> 좋아요 눌린 명언의 ID (quoteId)
    // 예: POKE 알림이면 -> 찌른 사람의 ID (memberId)
    private Long targetId;

    public void markAsRead() {
        this.readDate = LocalDateTime.now();
    }
}