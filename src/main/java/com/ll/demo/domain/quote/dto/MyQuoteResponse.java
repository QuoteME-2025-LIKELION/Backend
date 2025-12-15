package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.quote.entity.Quote;

public record MyQuoteResponse(
        String content,
        String groupName,
        String authorNickname,
        String birthYear
) {
    public static MyQuoteResponse from(Quote quote, String groupName) {
        return new MyQuoteResponse(
                quote.getContent(),
                groupName,
                quote.getAuthor().getNickname(),
                quote.getAuthor().getBirthYear()
        );
    }
}