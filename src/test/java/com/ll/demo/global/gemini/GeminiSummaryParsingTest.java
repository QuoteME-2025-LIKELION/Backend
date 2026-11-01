package com.ll.demo.global.gemini;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiSummaryParsingTest {

    @Test
    @DisplayName("정상 JSON 배열")
    void normal() {
        assertThat(GeminiService.parseSummaries("[\"하나\", \"둘\", \"셋\"]")).containsExactly("하나", "둘", "셋");
    }

    @Test
    @DisplayName("코드펜스 제거")
    void codeFence() {
        assertThat(GeminiService.parseSummaries("```json\n[\"하나\", \"둘\", \"셋\"]\n```"))
                .containsExactly("하나", "둘", "셋");
        assertThat(GeminiService.parseSummaries("```\n[\"하나\"]\n```")).containsExactly("하나");
    }

    @Test
    @DisplayName("2개면 2개, 4개 이상이면 3개까지")
    void countLimits() {
        assertThat(GeminiService.parseSummaries("[\"하나\", \"둘\"]")).containsExactly("하나", "둘");
        assertThat(GeminiService.parseSummaries("[\"1\", \"2\", \"3\", \"4\"]")).containsExactly("1", "2", "3");
    }

    @Test
    @DisplayName("30자 초과·공백·문자열 아닌 항목 제외")
    void filtersInvalidItems() {
        String thirty = "가".repeat(30);
        assertThat(GeminiService.parseSummaries("[\"" + thirty + "\", \"" + thirty + "가\", \"  \", 1, null, \"둘\"]"))
                .containsExactly(thirty, "둘");
    }

    @Test
    @DisplayName("30자 기준은 코드포인트")
    void lengthIsCodePoints() {
        String thirtyEmoji = "😀".repeat(30);
        assertThat(GeminiService.parseSummaries("[\"" + thirtyEmoji + "\", \"" + thirtyEmoji + "😀\"]"))
                .containsExactly(thirtyEmoji);
    }

    @Test
    @DisplayName("JSON 배열이 아니면 빈 목록")
    void notAnArray() {
        assertThat(GeminiService.parseSummaries("죄송합니다, 생성할 수 없습니다.")).isEmpty();
        assertThat(GeminiService.parseSummaries("{\"a\":1}")).isEmpty();
        assertThat(GeminiService.parseSummaries(null)).isEmpty();
    }

    @Test
    @DisplayName("빈 배열")
    void emptyArray() {
        assertThat(GeminiService.parseSummaries("[]")).isEmpty();
    }
}
