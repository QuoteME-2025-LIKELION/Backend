package com.ll.demo.domain.notice.entity;

import com.ll.demo.global.jpa.entity.BaseTime;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
