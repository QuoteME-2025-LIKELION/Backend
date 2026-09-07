package com.ll.demo.domain.notification.entity;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSetting extends BaseTime {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", unique = true, nullable = false)
    private Member member;

    // 그룹 관련 알림 (GROUP_INVITE, JOIN_REQUEST 등)
    @Builder.Default
    @Column(nullable = false)
    private boolean groupEnabled = true;

    // 친구 관련 알림 (FRIEND_REQUEST 등)
    @Builder.Default
    @Column(nullable = false)
    private boolean friendEnabled = true;

    // 태그 관련 알림 (TAG, TAG_REQUEST, TAG_ACCEPTED)
    @Builder.Default
    @Column(nullable = false)
    private boolean tagEnabled = true;

    // 콕 찌르기 알림 (POKE)
    @Builder.Default
    @Column(nullable = false)
    private boolean pokeEnabled = true;

    // 좋아요 알림 (LIKE)
    @Builder.Default
    @Column(nullable = false)
    private boolean likeEnabled = true;

    // 오늘의 명언 남기기 알림 (daily quote reminder)
    @Builder.Default
    @Column(nullable = false)
    private boolean quoteReminderEnabled = true;

    // 마케팅 정보 수신 알림
    @Builder.Default
    @Column(nullable = false)
    private boolean marketingEnabled = false;
}
