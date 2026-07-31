package com.ll.demo.domain.notice.dto;

import com.ll.demo.domain.notice.entity.Notice;

public record NoticeDetailResponse(
        Long id,
        String type,
        String title,
        String content,
        String createDate,
        String modifyDate
) {
    public NoticeDetailResponse(Notice notice) {
        this(
                notice.getId(),
                notice.getType().name(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreateDate().toString(),
                notice.getModifyDate() != null ? notice.getModifyDate().toString() : null
        );
    }
}
