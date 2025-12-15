package com.ll.demo.domain.quote.dto;

import java.util.List;

public record QuoteListDto(
        List<MyQuoteResponse> myQuotes,
        List<QuoteDetailResponse> otherQuotes
) {}