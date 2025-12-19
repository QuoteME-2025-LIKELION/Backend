package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.quote.entity.Quote;
import java.util.List;

public record MyQuoteResponse(
        Long id,
        String content,
        String groupName,
        String authorNickname,
        String birthYear,
        List<String> taggedNicknames
) {
    public static MyQuoteResponse from(Quote quote, String groupName) {
        return new MyQuoteResponse(
                quote.getId(),
                quote.getContent(),
                groupName,
                quote.getAuthor().getNickname(),
                quote.getAuthor().getBirthYear(),
                quote.getQuoteTags().stream()
                        .map(qt -> qt.getMember().getNickname())
                        .toList()
        );
    }
}