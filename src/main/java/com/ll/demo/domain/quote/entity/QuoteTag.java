package com.ll.demo.domain.quote.entity;

import com.ll.demo.domain.member.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QuoteTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Quote quote; // 태그된 글

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member; // 태그된 사람

    // 생성자
    public QuoteTag(Quote quote, Member member) {
        this.quote = quote;
        this.member = member;
    }
}