package com.ll.demo.domain.member.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// 앱 소셜 로그인 교환 응답 (쿠키 없음)
public record OAuth2TokenRespBody(
        String accessToken,
        String refreshToken,
        @JsonProperty("isNewUser") boolean isNewUser,
        boolean profileCompleted,
        MemberDto item
) {
}
