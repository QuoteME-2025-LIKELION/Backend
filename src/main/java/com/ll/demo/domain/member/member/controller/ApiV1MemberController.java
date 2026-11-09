package com.ll.demo.domain.member.member.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ll.demo.AppConfig;
import com.ll.demo.domain.member.member.dto.*;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.repository.MemberRepository;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.global.security.SecurityUser;
import com.ll.demo.standard.rq.Rq;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class ApiV1MemberController {

    private final MemberService memberService;
    private final Rq rq;
    private final AuthTokenService authTokenService;
    private final MemberRepository memberRepository;

    private static final int REFRESH_TOKEN_COOKIE_SEC = 60 * 60 * 24 * 30;

    private record IssuedTokens(String accessToken, String refreshToken) {
    }

    // 로그인 공통: 토큰 발급, 웹이면 쿠키 설정 (앱은 refresh 토큰을 본문으로 받고 쿠키를 쓰지 않음)
    private IssuedTokens issueTokens(Member member, boolean app, HttpServletResponse response) {
        String accessToken = authTokenService.genToken(member, AppConfig.getAccessTokenExpirationSec());
        String refreshToken = memberService.genRefreshToken(member);

        if (!app) {
            rq.setCookie(response, "accessToken", accessToken, AppConfig.getAccessTokenExpirationSec());
            rq.setCookie(response, "refreshToken", refreshToken, REFRESH_TOKEN_COOKIE_SEC);
        }
        return new IssuedTokens(accessToken, refreshToken);
    }

    // 회원가입
    @PostMapping("/signup")
    public RsData<MemberJoinRespBody> signup(@Valid @RequestBody MemberJoinReqBody body, HttpServletResponse response) {
        RsData<Member> joinRs = memberService.join(
                body.getEmail(),
                body.getPassword(),
                body.getBirthYear()
        );

        if (joinRs.getData() == null) return (RsData) joinRs;

        Member member = joinRs.getData();

        // 로그인과 똑같이
        boolean app = rq.isAppClient();
        IssuedTokens tokens = issueTokens(member, app, response);

        SecurityUser securityUser = new SecurityUser(member, member.getEmail(), "", member.getAuthorities());
        Authentication authentication = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return RsData.of("200-1", new MemberJoinRespBody(MemberDto.of(member), tokens.accessToken(), app ? tokens.refreshToken() : null));
    }

    @Getter
    @AllArgsConstructor
    public static class LoginResponseBody {
        private MemberDto item;
        private String accessToken;
        // 앱 요청(X-Client-Type: app)에만 채움, 웹 응답에는 키가 없음
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String refreshToken;

        public LoginResponseBody(MemberDto item, String accessToken) {
            this(item, accessToken, null);
        }
    }

    @PostMapping("/login")
    public RsData<LoginResponseBody> login(@Valid @RequestBody MemberLoginReqBody body, HttpServletResponse response) {
        Member member = memberService.findByEmail(body.getEmail())
                .orElseThrow(() -> new GlobalException("404-1", "존재하지 않는 계정입니다. 회원가입으로 이동하시겠어요?"));

        if (!memberService.checkPassword(member, body.getPassword())) {
            throw new GlobalException("401-1", "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        boolean app = rq.isAppClient();
        IssuedTokens tokens = issueTokens(member, app, response);

        // SecurityContext에 인증 정보 수동 등록 > 로그인 직후 null
        SecurityUser securityUser = new SecurityUser(member, member.getEmail(), "", member.getAuthorities());
        Authentication authentication = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return RsData.of("200-1", new LoginResponseBody(MemberDto.of(member), tokens.accessToken(), app ? tokens.refreshToken() : null));
    }

    // 본문 refreshToken 우선, 없으면 쿠키
    private String resolveRefreshToken(RefreshTokenReqBody body) {
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return body.refreshToken();
        }
        return rq.getCookieValue("refreshToken", "");
    }

    @PostMapping("/logout")
    public RsData<Void> logout(
            @RequestBody(required = false) RefreshTokenReqBody body,
            HttpServletResponse response,
            @AuthenticationPrincipal SecurityUser user) {
        // 서버에 저장된 refresh 토큰 무효화 (access 토큰 만료 상태에서도 본문·쿠키로 처리)
        memberService.clearRefreshToken(
                resolveRefreshToken(body),
                user != null ? user.getMember().getId() : null
        );

        // 브라우저 쿠키 삭제
        if (!rq.isAppClient()) {
            rq.setCookie(response, "accessToken", "", 0);
            rq.setCookie(response, "refreshToken", "", 0);
        }

        return RsData.of("200-2", null);
    }

    // ApiV1MemberController.java 내 guestLogin 메서드 보완

    @PostMapping("/guest-login")
    public RsData<LoginResponseBody> guestLogin(HttpServletResponse response) {
        // MemberService에서 더미 데이터가 포함된 계정 생성or가져오기
        Member guest = memberService.findOrCreateGuest();

        log.info("Guest login initiated: {}", guest.getEmail());

        // 토큰 생성-AppConfig 설정값
        boolean app = rq.isAppClient();
        IssuedTokens tokens = issueTokens(guest, app, response);

        // SecurityContext 등록 - 즉시 인증 확인
        SecurityUser securityUser = new SecurityUser(guest, guest.getEmail(), "", guest.getAuthorities());
        Authentication auth = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return RsData.of("200-1", new LoginResponseBody(MemberDto.of(guest), tokens.accessToken(), app ? tokens.refreshToken() : null));
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public RsData<LoginResponseBody> refresh(
            @RequestBody(required = false) RefreshTokenReqBody body,
            HttpServletResponse response) {
        // refresh 토큰은 회전하지 않음 (회원당 1개, 새 로그인 때만 교체)
        String refreshToken = resolveRefreshToken(body);

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GlobalException("401-2", "리프레시 토큰이 쿠키에 없습니다.");
        }

        Member member = memberService.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new GlobalException("401-3", "유효하지 않은 리프레시 토큰입니다."));

        String newAccessToken = authTokenService.genToken(member, AppConfig.getAccessTokenExpirationSec());

        if (!rq.isAppClient()) {
            rq.setCookie(response, "accessToken", newAccessToken, AppConfig.getAccessTokenExpirationSec());
        }

        return RsData.of("200-3", new LoginResponseBody(MemberDto.of(member), newAccessToken));
    }

    // 다른 회원 프로필
    @RestController
    @RequestMapping("/api/profile") // 에러가 났던 주소
    @RequiredArgsConstructor
    public class MemberController {

        private final MemberService memberService; // 기존에 있던 MemberService 주입

        @GetMapping("/{id}")
        public ResponseEntity<MemberResponse> getMemberProfile(@PathVariable Long id) {
            // memberService에서 회원을 찾아 DTO로 변환하여 반환하는 로직 필요
            Member member = memberRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));

            return ResponseEntity.ok(new MemberResponse(member));
        }
    }
}