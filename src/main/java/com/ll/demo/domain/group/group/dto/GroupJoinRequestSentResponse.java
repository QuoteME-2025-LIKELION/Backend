package com.ll.demo.domain.group.group.dto;

import com.ll.demo.domain.group.group.entity.GroupJoinRequest;

import java.time.LocalDateTime;

// 내가 보낸 대기 중인 가입 신청 (취소 버튼용)
public record GroupJoinRequestSentResponse(
        Long requestId,
        Long groupId,
        String groupName,
        LocalDateTime requestedAt
) {
    public static GroupJoinRequestSentResponse from(GroupJoinRequest req) {
        return new GroupJoinRequestSentResponse(
                req.getId(),
                req.getGroup().getId(),
                req.getGroup().getName(),
                req.getCreateDate()
        );
    }
}
