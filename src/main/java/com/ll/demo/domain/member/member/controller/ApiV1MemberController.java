package com.ll.demo.domain.member.member.controller;

import com.ll.demo.AppConfig;
import com.ll.demo.domain.member.member.dto.MemberDto;
import com.ll.demo.domain.member.member.dto.MemberJoinReqBody;
import com.ll.demo.domain.member.member.dto.MemberJoinRespBody;
import com.ll.demo.domain.member.member.dto.MemberLoginReqBody;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.AuthTokenService;
import com.ll.demo.standard.rq.Rq;
import com.ll.demo.global.security.SecurityUser;
import com.ll.demo.domain.member.member.dto.MemberJoinRespBody;
import com.ll.demo.domain.member.member.dto.SearchCombinedResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class ApiV1MemberController {

    private final MemberService memberService;
    private final Rq rq;
    private final AuthTokenService authTokenService;

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
        String accessToken = authTokenService.genToken(member, AppConfig.getAccessTokenExpirationSec());
        String refreshToken = memberService.genRefreshToken(member);

        rq.setCookie(response, "accessToken", accessToken, AppConfig.getAccessTokenExpirationSec());
        rq.setCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 30);

        SecurityUser securityUser = new SecurityUser(member, member.getEmail(), "", member.getAuthorities());
        Authentication authentication = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return RsData.of("200-1", new MemberJoinRespBody(MemberDto.of(member), accessToken));
    }

    @Getter
    @AllArgsConstructor
    public static class LoginResponseBody {
        private MemberDto item;
        private String accessToken;
    }

    @PostMapping("/login")
    public RsData<LoginResponseBody> login(@Valid @RequestBody MemberLoginReqBody body, HttpServletResponse response) {
        Member member = memberService.findByEmail(body.getEmail()).orElseThrow(() -> new RuntimeException("해당 회원을 찾을 수 없습니다."));

        String accessToken = authTokenService.genToken(member, AppConfig.getAccessTokenExpirationSec());
        String refreshToken = memberService.genRefreshToken(member);

        rq.setCookie(response, "accessToken", accessToken, AppConfig.getAccessTokenExpirationSec());
        rq.setCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 30);

        // SecurityContext에 인증 정보 수동 등록 > 로그인 직후 null
        SecurityUser securityUser = new SecurityUser(member, member.getEmail(), "", member.getAuthorities());
        Authentication authentication = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return RsData.of("200-1", new LoginResponseBody(MemberDto.of(member), accessToken));
    }

    @PostMapping("/logout")
    public RsData<Void> logout(HttpServletResponse response, @AuthenticationPrincipal SecurityUser user) {
        // 브라우저 쿠키 삭제
        rq.setCookie(response, "accessToken", "", 0);
        rq.setCookie(response, "refreshToken", "", 0);

        return RsData.of("200-2", null);
    }

    // 회원 및 그룹 통합 검색
    @GetMapping("/search")
    public ResponseEntity<SearchCombinedResponse> searchMembers(
            @RequestParam(value = "keyword", required = true) String keyword,
            @AuthenticationPrincipal SecurityUser securityUser
    ) {
        if (securityUser == null) {
            throw new GlobalException("401", "로그인이 필요합니다.");
        }

        Long currentMemberId = securityUser.getMember().getId();

        SearchCombinedResponse response = memberService.searchCombined(keyword, currentMemberId);
        return ResponseEntity.ok(response);
    }
}