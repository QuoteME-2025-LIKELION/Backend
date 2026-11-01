package com.ll.demo.domain.notice.dto;

import com.ll.demo.domain.notice.entity.Notice;

public record NoticeResponse(
        Long id,
        String type,
        String title,
        String content,
        String createDate,
        boolean important
) {
    public NoticeResponse(Notice notice) {
        this(
                notice.getId(),
                notice.getType().name(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreateDate().toString(),
                notice.isImportant()
        );
    }
}
