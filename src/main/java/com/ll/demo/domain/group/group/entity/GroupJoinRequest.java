package com.ll.demo.domain.group.group.entity;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GroupJoinRequest extends BaseTime {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    // 기존 DB 호환성을 위해 nullable 유지.
    // 신규 생성 초대는 반드시 inviter를 저장하고,
    // 기존 데이터는 별도 backfill 로 보완한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = true)
    private Member inviter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private Member requester;

    @Enumerated(EnumType.STRING)
    private JoinStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InviteType type = InviteType.JOIN_REQUEST;

    public void accept() { this.status = JoinStatus.ACCEPTED; }
    public void reject() { this.status = JoinStatus.REJECTED; }
}