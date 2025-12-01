package com.ll.demo.domain.quote.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 생성일 자동 주입을 위해 필요
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content; // 최종 명언 (예: "가끔은 쉬어 갈 때도 필요하다")

    @Column(columnDefinition = "TEXT")
    private String originalContent; // (옵션) 원본 일기 내용. null일 수 있음.

    // 현재는 id값만 저장하지만, 추후 Member 엔티티와 연관관계 매핑을 권장합니다.
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "user_id")
    // private Member author;
    @Column(nullable = false)
    private Long authorId; // 작성자 ID (Member의 ID)

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Builder
    public Quote(String content, Long authorId) {
        this.content = content;
        this.authorId = authorId;
    }
}