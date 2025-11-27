package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.quote.entity.Quote;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class QuoteResponse {
    private Long id;
    private String content;
    private Long authorId;
    private LocalDateTime createDate;

    public static QuoteResponse from(Quote quote) {
        return QuoteResponse.builder()
                .id(quote.getId())
                .content(quote.getContent())
                .authorId(quote.getAuthorId())
                .createDate(quote.getCreateDate())
                .build();
    }
}