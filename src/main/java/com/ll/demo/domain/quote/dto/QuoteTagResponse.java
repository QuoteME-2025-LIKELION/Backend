package com.ll.demo.domain.quote.dto;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.quote.entity.QuoteTag;

public record QuoteTagResponse(
        Long memberId,
        String nickname,
        String profileImage
) {
    public static QuoteTagResponse from(QuoteTag quoteTag) {
        Member member = quoteTag.getMember();
        return new QuoteTagResponse(member.getId(), member.getNickname(), member.getProfileImage());
    }
}
