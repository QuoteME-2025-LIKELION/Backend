package com.ll.demo.domain.group.group.dto;

public record GroupResponse(
        Long id,
        String name,
        String motto,
        String leaderNickname,
        long memberCount
) { }