package com.ll.demo.domain.member.member.dto;

// 앱 교환 코드·연동 결과 코드 + PKCE verifier
public record OAuth2TokenReqBody(String code, String codeVerifier) {
}
