package com.ll.demo.domain.group.group.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GroupDetailResponse(
        Long id,
        String name,
        String motto,
        String leaderNickname,
        long memberCount,
        long totalQuoteCount,
        LocalDateTime createdAt,
        List<MemberInfo> members
) {
    public record MemberInfo(
            Long id,
            String nickname,
            String profileImage,
            String introduction
    ) {}
}