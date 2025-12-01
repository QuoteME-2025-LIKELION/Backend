package com.ll.demo.domain.quote.entity;

import com.ll.demo.domain.member.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuoteLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    // 생성자 메서드
    public QuoteLike(Quote quote, Member member) {
        this.quote = quote;
        this.member = member;
    }
}