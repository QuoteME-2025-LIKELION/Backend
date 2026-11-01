package com.ll.demo.domain.member.member.dto;

import com.ll.demo.domain.member.member.entity.Member;

public record SocialLoginResult(Member member, boolean newUser) {
}
