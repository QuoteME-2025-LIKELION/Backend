package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.quote.entity.Quote;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record QuoteDetailResponse(
        Long id,
        String content,
        List<String> taggedNicknames,
        String authorNickname,
        String authorBirthYear,
        String authorProfileImage,
        String authorIntroduction,
        String timeAgo,
        boolean isLiked,
        boolean isBookmarked,
        boolean isFriendQuote,
        String myTagStatus,
        List<QuoteTagResponse> tags,
        @Schema(description = "summary(AI 추천 명언)가 null·빈 문자열·공백이 아니면 true")
        @JsonProperty("isAiGenerated") boolean isAiGenerated
) { public static QuoteDetailResponse from(
            Quote quote,
            List<String> taggedNicknames,
            boolean isLiked,
            boolean isBookmarked,
            boolean isFriend,
            String myTagStatus,
            List<QuoteTagResponse> tags
    ) {
        Duration duration = Duration.between(quote.getCreateDate(), LocalDateTime.now());
        String timeAgo = formatDuration(duration);

        String content = (quote.getSummary() != null && !quote.getSummary().isBlank())
                ? quote.getSummary()
                : quote.getContent();

        return new QuoteDetailResponse(
                quote.getId(),
                content,
                taggedNicknames,
                quote.getAuthor().getNickname(),
                quote.getAuthor().getBirthYear(),
                quote.getAuthor().getProfileImage(),
                quote.getAuthor().getIntroduction(),
                timeAgo,
                isLiked,
                isBookmarked,
                isFriend,
                myTagStatus,
                tags,
                quote.getSummary() != null && !quote.getSummary().isBlank()
        );
    }

    private static String formatDuration(Duration duration) {
        long days = duration.toDays();
        if (days > 0) return days + "일 전";

        long hours = duration.toHours();
        if (hours > 0) return hours + "시간 전";

        long minutes = duration.toMinutes();
        if (minutes > 0) return minutes + "분 전";

        return "방금 전";
    }
}
