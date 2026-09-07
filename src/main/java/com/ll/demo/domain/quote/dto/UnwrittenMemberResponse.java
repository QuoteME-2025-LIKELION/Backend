package com.ll.demo.domain.quote.dto;

public record UnwrittenMemberResponse(
        Long memberId,
        String nickname,
        String profileImage
) {}
