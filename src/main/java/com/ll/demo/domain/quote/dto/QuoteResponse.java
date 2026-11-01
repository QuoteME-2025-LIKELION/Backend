package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.quote.entity.Quote;
import java.time.LocalDateTime;
import java.util.List;

public record QuoteResponse(
        Long id,
        String content,
        String originalContent,
        String summary,
        String authorName,
        Integer authorBirthYear,
        List<String> taggedMemberNames,
        LocalDateTime createDate
) {
    public QuoteResponse(Quote quote) {
        this(
                quote.getId(),
                quote.getContent(),
                quote.getOriginalContent(),
                quote.getSummary(),
                quote.getAuthor().getNickname(),
                parseBirthYear(quote.getAuthor().getBirthYear()),
                quote.getQuoteTags().stream()
                                .map(tag -> tag.getMember().getName())
                                .toList(),
                quote.getCreateDate()
        );
    }

    // 과거에 저장된 숫자가 아닌 값은 null로 응답
    private static Integer parseBirthYear(String birthYear) {
        return birthYear != null && birthYear.matches("\\d{1,9}") ? Integer.valueOf(birthYear) : null;
    }

    public static QuoteResponse from(Quote quote) {
        return new QuoteResponse(quote);
    }
}