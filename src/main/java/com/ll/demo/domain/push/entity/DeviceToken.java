package com.ll.demo.domain.push.entity;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

// FCM 기기 토큰 (한 회원이 여러 기기를 가질 수 있고, 토큰은 한 회원에게만 속한다)
@Entity
@Table(name = "device_tokens")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class DeviceToken extends BaseTime {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    private LocalDateTime lastSeenAt;

    // 같은 기기에서 다른 계정으로 로그인하면 소유자를 옮긴다
    public void reassign(Member member, DevicePlatform platform, LocalDateTime seenAt) {
        this.member = member;
        this.platform = platform;
        this.lastSeenAt = seenAt;
    }
}
