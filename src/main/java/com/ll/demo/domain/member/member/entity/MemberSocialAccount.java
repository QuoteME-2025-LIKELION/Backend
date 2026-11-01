package com.ll.demo.domain.member.member.entity;

import com.ll.demo.domain.member.member.type.MemberProvider;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member_social_accounts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_id"}),
        @UniqueConstraint(columnNames = {"member_id", "provider"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberSocialAccount extends BaseTime {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;
}
