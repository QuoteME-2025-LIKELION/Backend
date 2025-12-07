package com.ll.demo.domain.group.group.entity;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "groups")
public class Group extends BaseTime {
    @Column(nullable = false, length = 10)
    private String name;

    @Column(length = 20) // 그룹 메시지
    private String motto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private Member leader;
}