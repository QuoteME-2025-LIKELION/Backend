package com.ll.demo.domain.member.member.dto;

// 앱 재발급·로그아웃 본문 (웹은 본문 없이 쿠키 사용)
public record RefreshTokenReqBody(String refreshToken) {
}
