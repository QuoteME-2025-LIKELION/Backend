package com.ll.demo.domain.member.member.dto;

import com.ll.demo.domain.member.member.entity.Member;

public record MemberResponse(
        Long id,
        String nickname,
        String profileImage,
        String introduction
) {
    public MemberResponse(Member member) {
        this(
                member.getId(),
                member.getNickname(),
                member.getProfileImage(),
                member.getIntroduction()
        );
    }
}