package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.Quote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteContentMappingTest {

    private Quote quote(String content, String summary) {
        Member author = Member.builder().nickname("작성자").build();
        Quote quote = Quote.builder().author(author).content(content).summary(summary).build();
        ReflectionTestUtils.setField(quote, "id", 1L);
        ReflectionTestUtils.setField(quote, "createDate", LocalDateTime.now());
        return quote;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("MyQuoteResponse: summary가 없으면 content를 반환한다")
    void myQuote_withoutSummary_returnsContent(String summary) {
        MyQuoteResponse response = MyQuoteResponse.from(quote("원문", summary), "그룹");

        assertThat(response.content()).isEqualTo("원문");
    }

    @Test
    @DisplayName("MyQuoteResponse: summary가 있으면 summary를 반환한다")
    void myQuote_withSummary_returnsSummary() {
        MyQuoteResponse response = MyQuoteResponse.from(quote("원문", "요약"), "그룹");

        assertThat(response.content()).isEqualTo("요약");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("QuoteDetailResponse: summary가 없으면 content를 반환한다")
    void detail_withoutSummary_returnsContent(String summary) {
        QuoteDetailResponse response = QuoteDetailResponse.from(quote("원문", summary), List.of(), false, false, true);

        assertThat(response.content()).isEqualTo("원문");
    }

    @Test
    @DisplayName("QuoteDetailResponse: summary가 있으면 summary를 반환한다")
    void detail_withSummary_returnsSummary() {
        QuoteDetailResponse response = QuoteDetailResponse.from(quote("원문", "요약"), List.of(), false, false, true);

        assertThat(response.content()).isEqualTo("요약");
    }
}