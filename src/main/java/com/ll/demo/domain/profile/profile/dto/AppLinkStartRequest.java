package com.ll.demo.domain.profile.profile.dto;

// 앱 계정 연동 시작: 복귀 URI(허용 목록) + PKCE(S256)
public record AppLinkStartRequest(String redirectUri, String codeChallenge, String codeChallengeMethod) {
}
