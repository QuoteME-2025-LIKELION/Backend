package com.ll.demo.domain.member.member.controller;

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
import jakarta.servlet.http.HttpServletRequest;
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
        Member member = memberService.findByEmail(body.getEmail())
                .orElseThrow(() -> new GlobalException("404-1", "존재하지 않는 계정입니다. 회원가입으로 이동하시겠어요?"));

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

    // ApiV1MemberController.java 내 guestLogin 메서드 보완

    @PostMapping("/guest-login")
    public RsData<LoginResponseBody> guestLogin(HttpServletResponse response) {
        // MemberService에서 더미 데이터가 포함된 계정 생성or가져오기
        Member guest = memberService.findOrCreateGuest();

        log.info("Guest login initiated: {}", guest.getEmail());

        // 토큰 생성-AppConfig 설정값
        String accessToken = authTokenService.genToken(guest, AppConfig.getAccessTokenExpirationSec());
        String refreshToken = memberService.genRefreshToken(guest);

        rq.setCookie(response, "accessToken", accessToken, AppConfig.getAccessTokenExpirationSec());
        rq.setCookie(response, "refreshToken", refreshToken, 60 * 60 * 24 * 30);

        // SecurityContext 등록 - 즉시 인증 확인
        SecurityUser securityUser = new SecurityUser(guest, guest.getEmail(), "", guest.getAuthorities());
        Authentication auth = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        return RsData.of("200-1", new LoginResponseBody(MemberDto.of(guest), accessToken));
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public RsData<LoginResponseBody> refresh(HttpServletRequest request, HttpServletResponse response) {
        //System.out.println("=== Cookie Debug Start ===");
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                System.out.println("Cookie Name: " + cookie.getName() + ", Value: " + cookie.getValue());
                if (cookie.getName().equals("refreshToken")) {
                    refreshToken = cookie.getValue();
                }
            }
        }
        System.out.println("Found RefreshToken: " + refreshToken);
        System.out.println("=== Cookie Debug End ===");

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GlobalException("401-2", "리프레시 토큰이 쿠키에 없습니다.");
        }

        Member member = memberService.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new GlobalException("401-3", "유효하지 않은 리프레시 토큰입니다."));

        String newAccessToken = authTokenService.genToken(member, AppConfig.getAccessTokenExpirationSec());

        rq.setCookie(response, "accessToken", newAccessToken, AppConfig.getAccessTokenExpirationSec());

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