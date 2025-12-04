package com.ll.demo.domain.member.member.entity;

import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friendships")
public class Friendship extends BaseTime {
    // 나
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 친구
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id")
    private Member friend;

    // 이후 상태 추가

    @Builder
    public Friendship(Member member, Member friend) {
        this.member = member;
        this.friend = friend;
    }
}