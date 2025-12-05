package com.ll.demo.domain.member.member.dto;

import com.ll.demo.domain.member.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileResponse {
    private Long id;
    private String email;
    private String nickname;
    private String introduction;
    private String profileImage; // 프로필 사진 URL

    public static ProfileResponse of(Member member) {
        return ProfileResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .introduction(member.getIntroduction())
                .profileImage(member.getProfileImage())
                .build();
    }
}