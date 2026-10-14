package com.ll.demo.domain.friendship.friendship.dto;

import com.ll.demo.domain.friendship.friendship.entity.Friendship;
import com.ll.demo.domain.friendship.friendship.type.FriendRequestDirection;
import com.ll.demo.domain.member.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendRequestResponse {
    private Long requestId;
    private FriendRequestDirection direction;
    private Long senderId;
    private String senderNickname;
    private String senderProfileImage;
    private String senderIntroduction;
    private Long receiverId;
    private String receiverNickname;
    private String receiverProfileImage;
    private String receiverIntroduction;

    public static FriendRequestResponse from(Friendship friendship, FriendRequestDirection direction) {
        Member sender = friendship.getMember();
        Member receiver = friendship.getFriend();
        return FriendRequestResponse.builder()
                .requestId(friendship.getId())
                .direction(direction)
                .senderId(sender.getId())
                .senderNickname(sender.getNickname())
                .senderProfileImage(sender.getProfileImage())
                .senderIntroduction(sender.getIntroduction())
                .receiverId(receiver.getId())
                .receiverNickname(receiver.getNickname())
                .receiverProfileImage(receiver.getProfileImage())
                .receiverIntroduction(receiver.getIntroduction())
                .build();
    }
}