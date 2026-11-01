package com.ll.demo.domain.group.group.dto;

import com.ll.demo.domain.group.group.entity.GroupJoinRequest;
import com.ll.demo.domain.member.member.entity.Member;

public record GroupJoinRequestResponse(
        Long requestId,
        Long requesterId,
        String requesterNickname,
        String requesterProfileImage,
        String requesterIntroduction
) {
    public static GroupJoinRequestResponse from(GroupJoinRequest req) {
        Member requester = req.getRequester();
        return new GroupJoinRequestResponse(
                req.getId(),
                requester.getId(),
                requester.getNickname(),
                requester.getProfileImage(),
                requester.getIntroduction()
        );
    }
}
