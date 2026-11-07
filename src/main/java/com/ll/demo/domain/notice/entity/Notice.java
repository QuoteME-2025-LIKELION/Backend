package com.ll.demo.domain.notice.entity;

import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class Notice extends BaseTime {

    @Enumerated(EnumType.STRING)
    private NoticeType type;

    private String title;

    private String content;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean important = false;

    // 노션에서 동기화된 공지만 값이 있다 (직접 등록·게스트 시드 공지는 null)
    @Column(unique = true)
    private String notionPageId;

    private LocalDateTime notionLastEditedAt;

    public void updateFromNotion(NoticeType type, String title, String content, boolean important, LocalDateTime lastEditedAt) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.important = important;
        this.notionLastEditedAt = lastEditedAt;
    }
}
