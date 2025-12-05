package com.ll.demo.domain.friendship.friendship.entity;

import com.ll.demo.domain.friendship.friendship.type.FriendshipStatus;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;
import com.ll.demo.domain.member.member.entity.Member;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    public void updateStatus(FriendshipStatus status) {
        this.status = status;
    }
}