package com.ll.demo.global.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class PagedResponse<T> {
    private final List<T> items;
    private final String emptyMessage;  // 빈 경우에만 값 존재, 아니면 아예 null 처리
    private final boolean isEmpty;

    private PagedResponse(List<T> items, String emptyMessage) {
        this.items = items;
        this.isEmpty = items.isEmpty();
        this.emptyMessage = items.isEmpty() ? emptyMessage : null;
    }

    // 데이터 있는 경우
    public static <T> PagedResponse<T> of(List<T> items) {
        return new PagedResponse<>(items, null);
    }

    // 빈 경우 — 메시지 명시
    public static <T> PagedResponse<T> empty(String message) {
        return new PagedResponse<>(List.of(), message);
    }

    // 데이터 유무에 따라 자동 분기
    public static <T> PagedResponse<T> from(List<T> items, String emptyMessage) {
        return new PagedResponse<>(items, emptyMessage);
    }
}