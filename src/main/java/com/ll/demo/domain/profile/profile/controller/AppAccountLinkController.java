package com.ll.demo.domain.profile.profile.controller;

import com.ll.demo.domain.member.member.dto.OAuth2TokenReqBody;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.profile.profile.dto.AppLinkConfirmResponse;
import com.ll.demo.domain.profile.profile.dto.AppLinkStartRequest;
import com.ll.demo.domain.profile.profile.dto.AppLinkStartResponse;
import com.ll.demo.domain.profile.profile.service.ProfileService;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.security.SecurityUser;
import com.ll.demo.global.security.oauth2.AppOAuth2Service;
import com.ll.demo.global.security.oauth2.OAuth2ExchangeCode;
import com.ll.demo.global.security.oauth2.OAuth2LoginModeFilter;
import com.ll.demo.global.security.oauth2.OAuth2MemberService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Set;

// 앱 계정 연동: 시작(Bearer) → 브라우저 진입(티켓) → 소셜 인증 → 앱 복귀 → 확정(Bearer + PKCE)
// 웹의 GET /api/profile/account/link/{provider}(쿠키 기반 즉시 연동)는 ProfileController 그대로
@Tag(name = "앱 인증")
@RestController
@RequiredArgsConstructor
public class AppAccountLinkController {

    private static final Set<String> LINKABLE_PROVIDERS = Set.of("google", "kakao");

    private final AppOAuth2Service appOAuth2Service;
    private final ProfileService profileService;

    @Value("${custom.site.backUrl:}")
    private String backUrl;

    @Operation(summary = "앱 계정 연동 시작", description = "authorizationUrl(5분, 1회용)을 시스템 인증 브라우저로 연다. 흐름은 상단 가이드 5절")
    @ApiResponse(responseCode = "400", description = "message 400-31: 복귀 URI 미허용·PKCE 오류")
    @ApiResponse(responseCode = "403", description = "게스트")
    @PostMapping("/api/profile/account/link/{provider}/start")
    public ResponseEntity<AppLinkStartResponse> startAppLink(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable String provider,
            @RequestBody(required = false) AppLinkStartRequest request
    ) {
        if (!LINKABLE_PROVIDERS.contains(provider)) {
            throw new GlobalException("400", "지원하지 않는 소셜 서비스입니다.");
        }
        Member member = securityUser.getMember();
        profileService.validateNotGuest(member);
        if (request == null || !appOAuth2Service.isValidAppRequest(
                request.redirectUri(), request.codeChallenge(), request.codeChallengeMethod())) {
            throw new GlobalException(AppOAuth2Service.INVALID_APP_REQUEST, AppOAuth2Service.INVALID_APP_REQUEST_MESSAGE);
        }

        String ticket = appOAuth2Service.issueLinkTicket(member.getId(), provider, request.redirectUri(), request.codeChallenge());
        String base = backUrl != null && !backUrl.isBlank()
                ? backUrl
                : ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        String authorizationUrl = UriComponentsBuilder.fromUriString(base)
                .path("/oauth2/link")
                .queryParam("ticket", ticket)
                .queryParam("provider", provider)
                .build().toUriString();
        return ResponseEntity.ok(new AppLinkStartResponse(authorizationUrl));
    }

    // 시스템 브라우저 진입 (인증 없음): 티켓을 1회 사용하고 세션에 연동 대상을 남긴 뒤 소셜 인증으로 이동
    @Hidden
    @GetMapping("/oauth2/link")
    public ResponseEntity<Void> enterAppLink(
            @RequestParam(required = false) String ticket,
            @RequestParam(required = false) String provider,
            HttpServletRequest request
    ) {
        OAuth2ExchangeCode linkTicket = appOAuth2Service.consumeLinkTicket(ticket, provider);

        HttpSession session = request.getSession();
        session.removeAttribute(OAuth2LoginModeFilter.SESSION_ATTRIBUTE);
        session.setAttribute(OAuth2MemberService.LINK_SESSION_ATTRIBUTE, linkTicket.getMemberId());
        AppOAuth2Service.startAppSession(session, linkTicket.getRedirectUri(), linkTicket.getCodeChallenge(), true);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/oauth2/authorization/" + linkTicket.getProvider()))
                .build();
    }

    // 확정: 이때 member_social_accounts에 저장
    @Operation(summary = "앱 계정 연동 확정", description = "복귀 시 받은 code(60초, 1회용)와 시작 때의 codeVerifier로 확정해야 연결이 저장됨")
    @ApiResponse(responseCode = "400", description = "message 400-30: 코드 없음·만료·재사용·verifier 불일치·다른 회원")
    @ApiResponse(responseCode = "409", description = "message 409-2: 확정 시점에 이미 다른 회원 또는 같은 서비스의 다른 계정에 연결됨")
    @PostMapping("/api/profile/account/link/confirm")
    public ResponseEntity<AppLinkConfirmResponse> confirmAppLink(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestBody(required = false) OAuth2TokenReqBody body
    ) {
        Member member = securityUser.getMember();
        profileService.validateNotGuest(member);
        appOAuth2Service.confirmLink(member.getId(),
                body != null ? body.code() : null,
                body != null ? body.codeVerifier() : null);
        return ResponseEntity.ok(new AppLinkConfirmResponse(profileService.getAccountInfo(member).linkedAccounts()));
    }
}
