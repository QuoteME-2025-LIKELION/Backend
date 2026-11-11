package com.ll.demo.domain.profile.profile.dto;

import com.ll.demo.domain.member.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileResponse(
        Long id,
        String email,
        String nickname,
        String introduction,
        String profileImage,
        long quoteCount,
        long friendCount,
        @Schema(description = "PUT /api/profile/setup으로 닉네임·출생년도를 저장하면 true. 신규 가입은 false, 기존 회원은 true")
        boolean profileCompleted
) {
    public static ProfileResponse from(Member member, long quoteCount, long friendCount) {
        return new ProfileResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getIntroduction(),
                member.getProfileImage(),
                quoteCount,
                friendCount,
                member.isProfileCompleted()
        );
    }
}