package com.ll.demo.domain.profile.profile.dto;

import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.type.Gender;
import com.ll.demo.domain.member.member.type.MemberProvider;

public record AccountInfoResponse(
        String email,
        String birthYear,
        Gender gender,
        MemberProvider provider,
        String providerId
) {
    public static AccountInfoResponse from(Member member) {
        return new AccountInfoResponse(
                member.getEmail(),
                member.getBirthYear(),
                member.getGender(),
                member.getProvider(),
                member.getProviderId()
        );
    }
}
