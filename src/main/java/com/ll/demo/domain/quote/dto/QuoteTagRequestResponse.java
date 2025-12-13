package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.quote.entity.QuoteTagRequest;

public record QuoteTagRequestResponse(
        Long requestId,      // ★ 이게 제일 중요! (수락/거절할 때 씀)
        Long requesterId,    // 요청한 사람 ID (프로필 이동용)
        String requesterName // 요청한 사람 이름 (화면 표시용)
) {
    public static QuoteTagRequestResponse from(QuoteTagRequest request) {
        return new QuoteTagRequestResponse(
                request.getId(),
                request.getRequester().getId(),
                request.getRequester().getName()
        );
    }
}