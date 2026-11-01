package com.ll.demo.domain.profile.profile.dto;

import com.ll.demo.domain.member.member.entity.Member;

public record ProfileResponse(
        Long id,
        String email,
        String nickname,
        String introduction,
        String profileImage,
        long quoteCount,
        long friendCount
) {
    public static ProfileResponse from(Member member, long quoteCount, long friendCount) {
        return new ProfileResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getIntroduction(),
                member.getProfileImage(),
                quoteCount,
                friendCount
        );
    }
}