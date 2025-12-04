package com.ll.demo.domain.member.member.dto;

import com.ll.demo.domain.member.member.entity.Member;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class MemberSearchResponse {
    private Long id;
    private String nickname;
    private String profileImage;
    private String email;
    // 추후 친구 정보 추가?
    public static MemberSearchResponse of(Member member) {
        return MemberSearchResponse.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .profileImage(member.getProfileImage())
                .email(member.getEmail())
                .build();
    }
}