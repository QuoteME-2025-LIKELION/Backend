package com.ll.demo.domain.member.member.dto;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendResponse {
    private Long id;
    private String nickname;
    private String profileImage;
    private String introduction;
    private boolean isGroupMember;

    public static FriendResponse of(Member friend, boolean isGroupMember) {
        return FriendResponse.builder()
                .id(friend.getId())
                .nickname(friend.getNickname())
                .profileImage(friend.getProfileImage())
                .introduction(friend.getIntroduction())
                .isGroupMember(isGroupMember)
                .build();
    }

    public static FriendResponse of(Friendship friendship) {
        Member friend = friendship.getFriend();
        return FriendResponse.of(friend, false); // 일단 false
    }
}