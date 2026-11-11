package com.ll.demo.domain.profile.profile.dto;

// 시스템 인증 브라우저로 열 연동 URL (티켓 5분, 1회용)
public record AppLinkStartResponse(String authorizationUrl) {
}
