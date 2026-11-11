package com.ll.demo.domain.member.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

// 앱 소셜 로그인 교환 응답 (쿠키 없음)
public record OAuth2TokenRespBody(
        String accessToken,
        String refreshToken,
        @Schema(description = "이번 소셜 인증으로 새로 가입한 회원이면 true")
        @JsonProperty("isNewUser") boolean isNewUser,
        @Schema(description = "PUT /api/profile/setup으로 닉네임·출생년도를 저장하면 true. 신규 가입은 false, 기존 회원은 true")
        boolean profileCompleted,
        MemberDto item
) {
}
