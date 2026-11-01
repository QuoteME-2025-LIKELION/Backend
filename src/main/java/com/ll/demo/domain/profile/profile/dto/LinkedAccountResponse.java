package com.ll.demo.domain.profile.profile.dto;

import com.ll.demo.domain.member.member.entity.MemberSocialAccount;
import com.ll.demo.domain.member.member.type.MemberProvider;

import java.time.LocalDateTime;

public record LinkedAccountResponse(
        MemberProvider provider,
        LocalDateTime linkedAt
) {
    public static LinkedAccountResponse from(MemberSocialAccount account) {
        return new LinkedAccountResponse(account.getProvider(), account.getCreateDate());
    }
}
