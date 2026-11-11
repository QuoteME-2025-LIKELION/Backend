package com.ll.demo.domain.profile.profile.dto;

import java.util.List;

// 앱 계정 연동 확정 결과
public record AppLinkConfirmResponse(List<LinkedAccountResponse> linkedAccounts) {
}
