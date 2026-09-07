package com.ll.demo.domain.group.group.dto;

import com.ll.demo.domain.group.group.entity.GroupJoinRequest;

public record GroupInviteResponse(
        Long requestId,
        Long groupId,
        String groupName,
        String groupMotto,
        String inviterNickname
) {
    public static GroupInviteResponse from(GroupJoinRequest req) {
        String inviterNickname = req.getInviter() != null
                ? req.getInviter().getNickname()
                : req.getGroup().getLeader().getNickname();

        return new GroupInviteResponse(
                req.getId(),
                req.getGroup().getId(),
                req.getGroup().getName(),
                req.getGroup().getMotto(),
                inviterNickname
        );
    }
}
