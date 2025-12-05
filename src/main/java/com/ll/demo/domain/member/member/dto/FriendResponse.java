package com.ll.demo.domain.member.member.dto;

import com.ll.demo.domain.member.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendResponse {
    private Long id;
    private String nickname;
    private String profileImage;
    private String email;

    public static FriendResponse of(Member member) {
        return FriendResponse.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .profileImage(member.getProfileImage())
                .email(member.getEmail())
                .build();
    }
}