package com.ll.demo.domain.quote.entity;

import static lombok.AccessLevel.PROTECTED;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

    // @AllArgsConstructor 사용으로 제거했습니다 - mj
//    // 생성자에서 Member를 받아서 author 필드에 넣습니다.
//    public Quote(Member author, String content, String originalContent) {
//        this.author = author;
//        this.content = content;
//        this.originalContent = originalContent;
//    }
}