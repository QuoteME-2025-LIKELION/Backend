package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.quote.entity.Quote;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record QuoteDetailResponse(
        Long id,
        String content,
        List<String> taggedNicknames, // 🟢 List<String> tags -> taggedNicknames으로 변경
        String authorNickname,
        String authorProfileImage,
        String authorIntroduction,
        String timeAgo,
        boolean isLiked,
        boolean isFriendQuote
) {
    public static QuoteDetailResponse from(
            Quote quote,
            List<String> taggedNicknames, // 🟢 태그된 닉네임 목록
            boolean isLiked,
            boolean isFriend
    ) {
        Duration duration = Duration.between(quote.getCreateDate(), LocalDateTime.now());
        String timeAgo = formatDuration(duration);

        return new QuoteDetailResponse(
                quote.getId(),
                quote.getContent(),
                taggedNicknames,
                quote.getAuthor().getNickname(),
                quote.getAuthor().getProfileImage(),
                quote.getAuthor().getIntroduction(),
                timeAgo,
                isLiked,
                isFriend
        );
    }

    // ... formatDuration 메서드는 이전과 동일
    private static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        if (hours > 0) return hours + "시간 전";
        long minutes = duration.toMinutes();
        if (minutes > 0) return minutes + "분 전";
        return "방금 전";
    }
}