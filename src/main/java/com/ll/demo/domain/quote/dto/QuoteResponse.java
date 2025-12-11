package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.quote.entity.Quote;
import java.time.LocalDateTime;
import java.util.List;

public record QuoteResponse(
        Long id,
        String content,
        String originalContent,
        String authorName,
        List<String> taggedMemberNames,
        LocalDateTime createDate
) {
    // Service에서 new QuoteResponse(quote)를 쓸 수 있게 해주는 생성자
    public QuoteResponse(Quote quote) {
        this(
                quote.getId(),
                quote.getContent(),
                quote.getOriginalContent(),
                quote.getAuthor().getNickname(),
                quote.getTags().stream()
                                .map(tag -> tag.getMember().getName())
                                .toList(),
                quote.getCreateDate()
        );
    }

    // (옵션) 기존 코드에 static 메서드가 있었다면 유지
    public static QuoteResponse from(Quote quote) {
        return new QuoteResponse(quote);
    }
}