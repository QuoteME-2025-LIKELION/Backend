package com.ll.demo.domain.profile.profile.dto;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.entity.MemberSocialAccount;
import com.ll.demo.domain.member.member.type.Gender;
import com.ll.demo.domain.member.member.type.MemberProvider;

import java.util.List;

public record AccountInfoResponse(
        String email,
        String birthYear,
        Gender gender,
        MemberProvider provider,
        String providerId,
        List<LinkedAccountResponse> linkedAccounts
) {
    public static AccountInfoResponse from(Member member, List<MemberSocialAccount> socialAccounts) {
        return new AccountInfoResponse(
                member.getEmail(),
                member.getBirthYear(),
                member.getGender(),
                member.getProvider(),
                member.getProviderId(),
                socialAccounts.stream().map(LinkedAccountResponse::from).toList()
        );
    }
}
