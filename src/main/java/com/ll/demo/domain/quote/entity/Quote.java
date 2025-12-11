package com.ll.demo.domain.quote.entity;

import static lombok.AccessLevel.PROTECTED;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Builder
public class Quote extends BaseTime {

    // ▼▼▼ 여기가 핵심입니다! ▼▼▼
    // "여러 개의 명언(Quote)은 한 명의 작성자(Member)에 의해 쓰인다" (N:1 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id") // DB에는 'author_id'라는 컬럼으로 저장됨
    private Member author;
    // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

    @Column(nullable = false)
    private String content; // 명언 내용

    @Column(columnDefinition = "TEXT")
    private String originalContent; // 원본 일기 내용

    // [추가] 조회할 때 태그된 정보를 쉽게 가져오기 위해 연결
    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Builder 패턴 쓸 때 초기화 방지
    private List<QuoteTag> tags = new ArrayList<>();

}