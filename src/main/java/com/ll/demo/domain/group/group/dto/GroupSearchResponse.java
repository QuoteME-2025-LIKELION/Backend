package com.ll.demo.domain.group.group.dto;

import com.ll.demo.domain.group.group.entity.Group;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupSearchResponse {
    private Long id;
    private String name;
    private String motto;
    private String leaderNickname;
    private long memberCount;

    public static GroupSearchResponse of(Group group) {
        return GroupSearchResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .motto(group.getMotto())
                .leaderNickname(group.getLeader().getNickname())
                .memberCount(group.getMembers() != null ? group.getMembers().size() : 0)
                .build();
    }
}